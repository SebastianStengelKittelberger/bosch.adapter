package de.kittelberger.bosch.adapter.data;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import de.kittelberger.bosch.adapter.mapping.MapCategoryDTOService;
import de.kittelberger.bosch.adapter.model.Category;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Indexes {@link Category} records into Elasticsearch.
 * Uses a dedicated index separate from the product index.
 * Only active when an {@link ElasticsearchClient} bean is present (i.e. {@code elasticsearch.enabled=true}).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ElasticsearchCategoryIndexService {

  private final ElasticsearchClient esClient;
  private final MapCategoryDTOService mapCategoryDTOService;

  @Value("${elasticsearch.category-index-prefix:bosch-categories}")
  private String indexPrefix;

  public ElasticsearchCategoryIndexService(
    ElasticsearchClient esClient,
    MapCategoryDTOService mapCategoryDTOService
  ) {
    this.esClient = esClient;
    this.mapCategoryDTOService = mapCategoryDTOService;
  }

  /**
   * Maps all categories for the given locale and bulk-indexes them into Elasticsearch.
   * The index is always recreated to ensure a clean, consistent state.
   *
   * @param country  two-letter country code (e.g. "DE")
   * @param language two-letter language code (e.g. "de")
   */
  public void indexCategories(String country, String language) {
    Locale locale = Locale.of(language, country);
    String indexName = indexPrefix + "-" + country.toLowerCase() + "-" + language.toLowerCase();

    log.info("Starting category indexing into ES index '{}'", indexName);
    recreateIndex(indexName);

    List<Category> categories = mapCategoryDTOService.map(locale);
    if (categories.isEmpty()) {
      log.info("No categories found for locale {}-{}", language, country);
      return;
    }

    BulkRequest.Builder bulk = new BulkRequest.Builder();
    categories.forEach(category -> bulk.operations(op -> op
      .index(i -> i
        .index(indexName)
        .id(category.ukey())
        .document(category)
      )
    ));

    try {
      BulkResponse response = esClient.bulk(bulk.build());
      if (response.errors()) {
        long failures = response.items().stream().filter(item -> item.error() != null).count();
        log.error("Bulk indexing to '{}' had {} failure(s) out of {} categories",
          indexName, failures, categories.size());
        response.items().stream()
          .filter(item -> item.error() != null)
          .forEach(item -> log.error("  Failed category '{}': {}", item.id(), item.error().reason()));
      } else {
        log.info("Indexed {} categories into ES index '{}'", categories.size(), indexName);
      }
    } catch (Exception e) {
      log.error("Failed to bulk-index {} categories to '{}'", categories.size(), indexName, e);
    }
  }

  private void recreateIndex(String indexName) {
    try {
      boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
      if (exists) {
        esClient.indices().delete(d -> d.index(indexName));
        log.info("Deleted existing ES index '{}' for full reindex", indexName);
      }
      esClient.indices().create(c -> c
        .index(indexName)
        .mappings(m -> m.dynamic(DynamicMapping.False))
      );
      log.info("Created ES index '{}' with dynamic mapping disabled", indexName);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to (re)create ES index '" + indexName + "'", e);
    }
  }
}
