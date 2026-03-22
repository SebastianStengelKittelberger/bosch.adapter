package de.kittelberger.bosch.adapter.data;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * Stores adapter configuration values (e.g. domain) in a dedicated Elasticsearch index.
 * Uses a single fixed document per config key (no locale).
 * Only active when an {@link ElasticsearchClient} bean is present.
 */
@Slf4j
@Service
@ConditionalOnBean(ElasticsearchClient.class)
public class ElasticsearchDomainIndexService {

  static final String DOC_ID = "domain";

  private final ElasticsearchClient esClient;

  @Value("${elasticsearch.config-index:bosch-config}")
  private String indexName;

  public ElasticsearchDomainIndexService(ElasticsearchClient esClient) {
    this.esClient = esClient;
  }

  public void indexDomain(String domain) {
    try {
      ensureIndexExists();
      esClient.index(i -> i
        .index(indexName)
        .id(DOC_ID)
        .document(Map.of("value", domain))
      );
      log.info("Indexed domain '{}' into ES index '{}'", domain, indexName);
    } catch (Exception e) {
      log.error("Failed to index domain into '{}'", indexName, e);
    }
  }

  private void ensureIndexExists() throws IOException {
    boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
    if (!exists) {
      esClient.indices().create(c -> c
        .index(indexName)
        .mappings(m -> m.dynamic(DynamicMapping.False))
      );
      log.info("Created ES index '{}'", indexName);
    }
  }
}
