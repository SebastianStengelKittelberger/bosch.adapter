package de.kittelberger.bosch.adapter.data;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import de.kittelberger.bosch.adapter.mapping.MapAttrDTOService;
import de.kittelberger.bosch.adapter.model.AttrClass;
import de.kittelberger.bosch.adapter.model.Reference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Indexes {@link Reference} records into Elasticsearch.
 * Uses a dedicated index separate from the product index to keep data cleanly separated.
 * Only active when an {@link ElasticsearchClient} bean is present (i.e. {@code elasticsearch.enabled=true}).
 */
@Slf4j
@Service
@ConditionalOnBean(ElasticsearchClient.class)
public class ElasticsearchReferenceIndexService {

  private final ElasticsearchClient esClient;
  private final MapAttrDTOService mapAttrDTOService;

  @Value("${elasticsearch.reference-index-prefix:bosch-references}")
  private String indexPrefix;

  public ElasticsearchReferenceIndexService(
    ElasticsearchClient esClient,
    MapAttrDTOService mapAttrDTOService
  ) {
    this.esClient = esClient;
    this.mapAttrDTOService = mapAttrDTOService;
  }

  /**
   * Loads all references for the given locale and bulk-indexes them into Elasticsearch.
   * The index is always recreated to ensure a clean, consistent state.
   *
   * @param country  two-letter country code (e.g. "DE")
   * @param language two-letter language code (e.g. "de")
   */
  public void indexReferences(String country, String language) {
    Locale locale = Locale.of(language, country);
    String indexName = indexPrefix + "-" + country.toLowerCase() + "-" + language.toLowerCase();

    log.info("Starting reference indexing into ES index '{}'", indexName);
    recreateIndex(indexName);

    List<Reference> references = mapAttrDTOService.map(locale);
    if (references.isEmpty()) {
      log.info("No references found for locale {}-{}", language, country);
      return;
    }

    BulkRequest.Builder bulk = new BulkRequest.Builder();
    references.forEach(ref -> bulk.operations(op -> op
      .index(i -> i
        .index(indexName)
        .id(ref.getUkey())
        .document(toDocument(ref))
      )
    ));

    try {
      BulkResponse response = esClient.bulk(bulk.build());
      if (response.errors()) {
        long failures = response.items().stream().filter(item -> item.error() != null).count();
        log.error("Bulk indexing to '{}' had {} failure(s) out of {} references",
          indexName, failures, references.size());
        response.items().stream()
          .filter(item -> item.error() != null)
          .forEach(item -> log.error("  Failed reference '{}': {}", item.id(), item.error().reason()));
      } else {
        log.info("Indexed {} references into ES index '{}'", references.size(), indexName);
      }
    } catch (Exception e) {
      log.error("Failed to bulk-index {} references to '{}'", references.size(), indexName, e);
    }
  }

  /**
   * Converts a {@link Reference} to a plain map with an explicit {@code attrClasses}
   * structure of {@code {left, right}} — bypassing Jackson 2's special handling of
   * {@code Map.Entry} (which {@link org.apache.commons.lang3.tuple.Pair} implements),
   * ensuring the stored JSON is compatible with illusion's {@code AttrClassRef} record.
   */
  private Map<String, Object> toDocument(Reference ref) {
    String left = ref.getAttrClasses() != null ? ref.getAttrClasses().getLeft() : "";
    List<AttrClass> attrClasses = ref.getAttrClasses() != null && ref.getAttrClasses().getRight() != null
      ? ref.getAttrClasses().getRight()
      : List.of();

    List<Map<String, String>> attrClassDocs = attrClasses.stream()
      .map(ac -> Map.of(
        "name", ac.getName() != null ? ac.getName() : "",
        "ukey", ac.getUkey() != null ? ac.getUkey() : ""
      ))
      .toList();

    Map<String, Object> doc = new HashMap<>();
    doc.put("id", ref.getId());
    doc.put("ukey", ref.getUkey());
    doc.put("attrClasses", Map.of("left", left, "right", attrClassDocs));
    return doc;
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
