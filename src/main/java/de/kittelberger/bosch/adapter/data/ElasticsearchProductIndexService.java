package de.kittelberger.bosch.adapter.data;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import de.kittelberger.bosch.adapter.mapping.MapProductDTOService;
import de.kittelberger.bosch.adapter.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Indexes bosch.adapter {@link Product} records into Elasticsearch.
 * Only active when an {@link ElasticsearchClient} bean is present (i.e. {@code elasticsearch.enabled=true}).
 *
 * <p>Products are streamed one-by-one from {@link MapProductDTOService#stream} and
 * bulk-indexed in configurable batches to avoid loading all products into memory.
 * The target index follows the pattern {@code {prefix}-{country}-{language}} (all lowercase).
 */
@Slf4j
@Service
@ConditionalOnBean(ElasticsearchClient.class)
public class ElasticsearchProductIndexService {

  private final ElasticsearchClient esClient;
  private final MapProductDTOService mapProductDTOService;

  @Value("${elasticsearch.product-index-prefix:bosch-products}")
  private String indexPrefix;

  @Value("${elasticsearch.batch-size:100}")
  private int batchSize;

  public ElasticsearchProductIndexService(
    ElasticsearchClient esClient,
    MapProductDTOService mapProductDTOService
  ) {
    this.esClient = esClient;
    this.mapProductDTOService = mapProductDTOService;
  }

  /**
   * Streams all products for the given locale and bulk-indexes them into Elasticsearch.
   *
   * @param country  two-letter country code (e.g. "DE")
   * @param language two-letter language code (e.g. "de")
   */
  public void indexProducts(String country, String language) {
    Locale locale = Locale.of(language, country);
    String indexName = indexPrefix + "-" + country.toLowerCase() + "-" + language.toLowerCase();
    List<Product> batch = new ArrayList<>(batchSize);

    log.info("Starting product indexing into ES index '{}'", indexName);

    recreateIndex(indexName);

    mapProductDTOService.stream(locale, product -> {
      if (product.productMetaData() == null) return;
      batch.add(product);
      if (batch.size() >= batchSize) {
        bulkIndex(indexName, new ArrayList<>(batch));
        batch.clear();
      }
    });

    if (!batch.isEmpty()) {
      bulkIndex(indexName, batch);
    }

    log.info("Finished product indexing into ES index '{}'", indexName);
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

  private void bulkIndex(String indexName, List<Product> products) {
    BulkRequest.Builder bulk = new BulkRequest.Builder();
    products.forEach(product -> {
      String id = product.productMetaData().getId().toString();
      bulk.operations(op -> op
        .index(i -> i
          .index(indexName)
          .id(id)
          .document(product)
        )
      );
    });

    try {
      BulkResponse response = esClient.bulk(bulk.build());
      if (response.errors()) {
        long failures = response.items().stream().filter(item -> item.error() != null).count();
        log.error(
          "Bulk indexing to '{}' had {} failure(s) out of {} products",
          indexName, failures, products.size()
        );
        response.items().stream()
          .filter(item -> item.error() != null)
          .forEach(item -> log.error("  Failed product '{}': {}", item.id(), item.error().reason()));
      } else {
        log.debug("Indexed {} products to '{}'", products.size(), indexName);
      }
    } catch (Exception e) {
      log.error("Failed to bulk-index {} products to '{}'", products.size(), indexName, e);
    }
  }
}
