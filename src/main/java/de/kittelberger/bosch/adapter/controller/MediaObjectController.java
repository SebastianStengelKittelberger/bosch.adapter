package de.kittelberger.bosch.adapter.controller;

import de.kittelberger.bosch.adapter.mapping.MapMediaObjectDTOService;
import de.kittelberger.bosch.adapter.model.MediaObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
public class MediaObjectController {

  private final MapMediaObjectDTOService mapMediaObjectDTOService;

  public MediaObjectController(MapMediaObjectDTOService mapMediaObjectDTOService) {
    this.mapMediaObjectDTOService = mapMediaObjectDTOService;
  }

  @GetMapping("{country}/{language}/media-objects")
  public List<MediaObject> getMediaObjects(
    @PathVariable String country,
    @PathVariable String language
  ) {
    Locale locale = Locale. of(language, country);
    return mapMediaObjectDTOService.map(locale);
  }
}
