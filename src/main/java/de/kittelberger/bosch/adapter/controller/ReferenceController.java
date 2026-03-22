package de.kittelberger.bosch.adapter.controller;

import de.kittelberger.bosch.adapter.data.ElasticsearchReferenceIndexService;
import de.kittelberger.bosch.adapter.mapping.MapAttrDTOService;
import de.kittelberger.bosch.adapter.model.Reference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
public class ReferenceController {

  private final MapAttrDTOService mapAttrDTOService;
  private final Optional<ElasticsearchReferenceIndexService> elasticsearchReferenceIndexService;

  public ReferenceController(
    MapAttrDTOService mapAttrDTOService,
    Optional<ElasticsearchReferenceIndexService> elasticsearchReferenceIndexService
  ) {
    this.mapAttrDTOService = mapAttrDTOService;
    this.elasticsearchReferenceIndexService = elasticsearchReferenceIndexService;
  }

  @GetMapping("/{country}/{language}/references")
  public List<Reference> getReferences(
    @PathVariable String country,
    @PathVariable String language
  ) {
    Locale locale = Locale.of(language, country);
    return mapAttrDTOService.map(locale);
  }

  /**
   * Indexes all references for the given locale into Elasticsearch.
   * Returns 503 if Elasticsearch is not configured.
   */
  @PostMapping("/{country}/{language}/index/references")
  public ResponseEntity<Map<String, Object>> indexReferences(
    @PathVariable String country,
    @PathVariable String language
  ) {
    if (elasticsearchReferenceIndexService.isEmpty()) {
      return ResponseEntity.status(503).body(Map.of(
        "error", "Elasticsearch not configured — set elasticsearch.enabled=true"
      ));
    }
    elasticsearchReferenceIndexService.get().indexReferences(country, language);
    return ResponseEntity.ok(Map.of(
      "status", "indexed",
      "index", "bosch-references-" + country.toLowerCase() + "-" + language.toLowerCase()
    ));
  }
}
