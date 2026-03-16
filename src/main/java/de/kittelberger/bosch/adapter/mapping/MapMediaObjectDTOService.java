package de.kittelberger.bosch.adapter.mapping;

import de.kittelberger.bosch.adapter.data.LoadDataService;
import de.kittelberger.bosch.adapter.model.Attribute;
import de.kittelberger.bosch.adapter.model.Image;
import de.kittelberger.bosch.adapter.model.MediaObject;
import de.kittelberger.bosch.adapter.model.MediaSpecifics;
import de.kittelberger.bosch.adapter.util.ClUtil;
import de.kittelberger.bosch.adapter.util.ImageUtil;
import de.kittelberger.bosch.adapter.util.UrlBuilderUtil;
import de.kittelberger.webexport602w.solr.api.dto.MediaobjectDTO;
import de.kittelberger.webexport602w.solr.api.generated.Attrval;
import de.kittelberger.webexport602w.solr.api.generated.Lobval;
import de.kittelberger.webexport602w.solr.api.generated.Mediaobject;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class MapMediaObjectDTOService {

  private final LoadDataService loadDataService;
  private final UrlBuilderUtil urlBuilderUtil = new UrlBuilderUtil();

  @Value( "${domain.media.default}")
  private String domain;
  private static final String LOBVALUE_TYP_LORES_PNG_RGB = "LORES_PNG_RGB";

  public MapMediaObjectDTOService(LoadDataService loadDataService) {
    this.loadDataService = loadDataService;
  }

  public List<Image> map() {
    return loadDataService.getMediaobjectDTOs().stream()
      .map(this::map)
      .filter(Objects::nonNull)
      .toList();
  }

  private Image map(final MediaobjectDTO dto) {
    return ImageUtil.getImageItemFromMediaObject(
      LOBVALUE_TYP_LORES_PNG_RGB,
      domain,
      dto,
      urlBuilderUtil
    );
  }
}
