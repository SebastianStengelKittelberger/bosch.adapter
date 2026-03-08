package de.kittelberger.bosch.adapter.controller;

import de.kittelberger.bosch.adapter.mapping.MapAttrDTOService;
import de.kittelberger.bosch.adapter.model.Reference;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
public class ReferenceController {

  private final MapAttrDTOService mapAttrDTOService;

  public ReferenceController(MapAttrDTOService mapAttrDTOService) {
    this.mapAttrDTOService = mapAttrDTOService;
  }

  @GetMapping("/{country}/{language}/references")
  public List<Reference> getReferences(
    @PathVariable String country,
    @PathVariable String language
  ) {
    Locale locale = Locale.of(language, country);
    return mapAttrDTOService.map(locale);
  }
}
