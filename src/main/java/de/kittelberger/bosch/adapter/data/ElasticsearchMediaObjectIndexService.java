package de.kittelberger.bosch.adapter.data;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import de.kittelberger.bosch.adapter.mapping.MapMediaObjectDTOService;
import de.kittelberger.bosch.adapter.model.Image;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Indexes {@link Image} (media object) records into a dedicated Elasticsearch index.
 * Media objects are locale-independent, so a single global index is used.
 * Only active when an {@link ElasticsearchClient} bean is present.
 */
@Slf4j
@Service
@ConditionalOnBean(ElasticsearchClient.class)
public class ElasticsearchMediaObjectIndexService {

  private final ElasticsearchClient esClient;
  private final MapMediaObjectDTOService mapMediaObjectDTOService;

  @Value("${elasticsearch.media-object-index:bosch-media-objects}")
  private String indexName;

  public ElasticsearchMediaObjectIndexService(
    ElasticsearchClient esClient,
    MapMediaObjectDTOService mapMediaObjectDTOService
  ) {
    this.esClient = esClient;
    this.mapMediaObjectDTOService = mapMediaObjectDTOService;
  }

  public void indexMediaObjects() {
    log.info("Starting media object indexing into ES index '{}'", indexName);
    recreateIndex();

    List<Image> images = mapMediaObjectDTOService.map();
    if (images.isEmpty()) {
      log.info("No media objects found");
      return;
    }

    int batchSize = 200;
    for (int i = 0; i < images.size(); i += batchSize) {
      List<Image> batch = images.subList(i, Math.min(i + batchSize, images.size()));
      bulkIndex(batch);
    }

    log.info("Finished media object indexing: {} image(s) into '{}'", images.size(), indexName);
  }

  private void bulkIndex(List<Image> images) {
    BulkRequest.Builder bulk = new BulkRequest.Builder();
    images.forEach(image -> bulk.operations(op -> op
      .index(i -> i
        .index(indexName)
        .id(image.getMediaObjectId() != null ? image.getMediaObjectId().toString() : image.getUkey())
        .document(image)
      )
    ));

    try {
      BulkResponse response = esClient.bulk(bulk.build());
      if (response.errors()) {
        long failures = response.items().stream().filter(item -> item.error() != null).count();
        log.error("Bulk indexing to '{}' had {} failure(s) out of {} images",
          indexName, failures, images.size());
        response.items().stream()
          .filter(item -> item.error() != null)
          .forEach(item -> log.error("  Failed image '{}': {}", item.id(), item.error().reason()));
      } else {
        log.debug("Indexed {} images to '{}'", images.size(), indexName);
      }
    } catch (Exception e) {
      log.error("Failed to bulk-index {} images to '{}'", images.size(), indexName, e);
    }
  }

  private void recreateIndex() {
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
