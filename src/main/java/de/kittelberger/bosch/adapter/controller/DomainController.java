package de.kittelberger.bosch.adapter.controller;

import de.kittelberger.bosch.adapter.data.ElasticsearchDomainIndexService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
public class DomainController {

  @Value("${domain.media.default}")
  private String mediaDomain;

  private final Optional<ElasticsearchDomainIndexService> elasticsearchDomainIndexService;

  public DomainController(Optional<ElasticsearchDomainIndexService> elasticsearchDomainIndexService) {
    this.elasticsearchDomainIndexService = elasticsearchDomainIndexService;
  }

  @GetMapping("/domain")
  public String getDomain() {
    return mediaDomain;
  }

  /**
   * Writes the configured media domain into Elasticsearch for illusion to consume.
   * Returns 503 if Elasticsearch is not configured.
   */
  @PostMapping("/index/domain")
  public ResponseEntity<Map<String, Object>> indexDomain() {
    if (elasticsearchDomainIndexService.isEmpty()) {
      return ResponseEntity.status(503).body(Map.of(
        "error", "Elasticsearch not configured — set elasticsearch.enabled=true"
      ));
    }
    elasticsearchDomainIndexService.get().indexDomain(mediaDomain);
    return ResponseEntity.ok(Map.of("status", "indexed", "value", mediaDomain));
  }
}
