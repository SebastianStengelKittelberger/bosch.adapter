package de.kittelberger.bosch.adapter.controller;

import de.kittelberger.bosch.adapter.data.ElasticsearchMediaObjectIndexService;
import de.kittelberger.bosch.adapter.mapping.MapMediaObjectDTOService;
import de.kittelberger.bosch.adapter.model.Image;
import de.kittelberger.bosch.adapter.model.MediaObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
public class MediaObjectController {

  private final MapMediaObjectDTOService mapMediaObjectDTOService;
  private final Optional<ElasticsearchMediaObjectIndexService> elasticsearchMediaObjectIndexService;

  public MediaObjectController(
    MapMediaObjectDTOService mapMediaObjectDTOService,
    Optional<ElasticsearchMediaObjectIndexService> elasticsearchMediaObjectIndexService
  ) {
    this.mapMediaObjectDTOService = mapMediaObjectDTOService;
    this.elasticsearchMediaObjectIndexService = elasticsearchMediaObjectIndexService;
  }

  @GetMapping("{country}/{language}/media-objects")
  public List<Image> getMediaObjects(
    @PathVariable String country,
    @PathVariable String language
  ) {
    return mapMediaObjectDTOService.map();
  }

  /**
   * Indexes all media objects into Elasticsearch.
   * Media objects are locale-independent — a single global index is used.
   * Returns 503 if Elasticsearch is not configured.
   */
  @PostMapping("{country}/{language}/index/media-objects")
  public ResponseEntity<Map<String, Object>> indexMediaObjects(
    @PathVariable String country,
    @PathVariable String language
  ) {
    if (elasticsearchMediaObjectIndexService.isEmpty()) {
      return ResponseEntity.status(503).body(Map.of(
        "error", "Elasticsearch not configured — set elasticsearch.enabled=true"
      ));
    }
    elasticsearchMediaObjectIndexService.get().indexMediaObjects();
    return ResponseEntity.ok(Map.of("status", "indexed", "index", "bosch-media-objects"));
  }
}
