package de.kittelberger.bosch.adapter.controller;

import de.kittelberger.bosch.adapter.mapping.MapProductDTOService;
import de.kittelberger.bosch.adapter.model.Product;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;

@RestController
public class BoschController {

  private final MapProductDTOService mapProductDTOService;
  private final ObjectMapper objectMapper;

  public BoschController(MapProductDTOService mapProductDTOService, ObjectMapper objectMapper) {
    this.mapProductDTOService = mapProductDTOService;
    this.objectMapper = objectMapper;
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

  private void writeProduct(JsonGenerator generator, Product product) {
    objectMapper.writeValue(generator, product);
  }
}
