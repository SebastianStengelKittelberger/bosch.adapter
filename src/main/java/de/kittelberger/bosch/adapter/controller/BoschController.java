package de.kittelberger.bosch.adapter.controller;

import de.kittelberger.bosch.adapter.data.ElasticsearchProductIndexService;
import de.kittelberger.bosch.adapter.mapping.MapProductDTOService;
import de.kittelberger.bosch.adapter.model.Product;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
public class BoschController {

  private final MapProductDTOService mapProductDTOService;
  private final ObjectMapper objectMapper;
  private final Optional<ElasticsearchProductIndexService> elasticsearchProductIndexService;

  public BoschController(
    MapProductDTOService mapProductDTOService,
    ObjectMapper objectMapper,
    Optional<ElasticsearchProductIndexService> elasticsearchProductIndexService
  ) {
    this.mapProductDTOService = mapProductDTOService;
    this.objectMapper = objectMapper;
    this.elasticsearchProductIndexService = elasticsearchProductIndexService;
  }

  @GetMapping(value = "/{country}/{language}/products", produces = MediaType.APPLICATION_JSON_VALUE)
  public StreamingResponseBody getProducts(
    @PathVariable String country,
    @PathVariable String language
  ) {
    Locale locale = Locale.of(language, country);
    return outputStream -> {
      try (JsonGenerator generator = objectMapper.createGenerator(outputStream)) {
        generator.writeStartArray();
        mapProductDTOService.stream(locale, product -> writeProduct(generator, product));
        generator.writeEndArray();
        generator.flush();
      } catch (UncheckedIOException e) {
        throw e.getCause();
      }
    };
  }

  /**
   * Indexes all products for the given locale into Elasticsearch.
   * Returns 503 if Elasticsearch is not configured.
   */
  @PostMapping("/{country}/{language}/index")
  public ResponseEntity<Map<String, Object>> indexProducts(
    @PathVariable String country,
    @PathVariable String language
  ) {
    if (elasticsearchProductIndexService.isEmpty()) {
      return ResponseEntity.status(503).body(Map.of(
        "error", "Elasticsearch not configured — set elasticsearch.enabled=true"
      ));
    }
    elasticsearchProductIndexService.get().indexProducts(country, language);
    return ResponseEntity.ok(Map.of(
      "status", "indexed",
      "index", "bosch-products-" + country.toLowerCase() + "-" + language.toLowerCase()
    ));
  }

  private void writeProduct(JsonGenerator generator, Product product) {
    objectMapper.writeValue(generator, product);
  }
}
