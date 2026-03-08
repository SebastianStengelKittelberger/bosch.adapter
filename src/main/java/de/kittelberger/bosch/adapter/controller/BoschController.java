package de.kittelberger.bosch.adapter.controller;

import de.kittelberger.bosch.adapter.mapping.MapProductDTOService;
import de.kittelberger.bosch.adapter.model.Product;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
public class BoschController {

  private final MapProductDTOService mapProductDTOService;

  public BoschController(MapProductDTOService mapProductDTOService) {
    this.mapProductDTOService = mapProductDTOService;
  }

  @GetMapping("/{country}/{language}/products")
  public List<Product> getProducts(
    @PathVariable String country,
    @PathVariable String language
  ) {
    Locale locale = Locale.of(language, country);
    return mapProductDTOService.map(locale);
  }
}
