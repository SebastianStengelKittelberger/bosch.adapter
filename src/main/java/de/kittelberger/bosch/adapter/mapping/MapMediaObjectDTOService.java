package de.kittelberger.bosch.adapter.mapping;

import de.kittelberger.bosch.adapter.data.LoadDataService;
import de.kittelberger.bosch.adapter.model.Attribute;
import de.kittelberger.bosch.adapter.model.MediaObject;
import de.kittelberger.bosch.adapter.model.MediaSpecifics;
import de.kittelberger.bosch.adapter.util.ClUtil;
import de.kittelberger.webexport602w.solr.api.dto.MediaobjectDTO;
import de.kittelberger.webexport602w.solr.api.generated.Attrval;
import de.kittelberger.webexport602w.solr.api.generated.Lobval;
import de.kittelberger.webexport602w.solr.api.generated.Mediaobject;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class MapMediaObjectDTOService {

  private final LoadDataService loadDataService;

  public MapMediaObjectDTOService(LoadDataService loadDataService) {
    this.loadDataService = loadDataService;
  }

  public List<MediaObject> map(final Locale locale) {
    return loadDataService.getMediaobjectDTOs().stream()
      .map(dto -> map(dto, locale))
      .toList();
  }

  private MediaObject map(final MediaobjectDTO dto, final Locale locale) {
    return new MediaObject(
      ClUtil.getCleanedValue(dto.getName(), locale),
      mapAttributes(dto, locale),
      mapMediaobjecttypeReferences(dto),
      mapMediaSpecifics(dto),
      dto.getId()
    );
  }

  private List<Attribute> mapAttributes(final MediaobjectDTO dto, final Locale locale) {
    if (dto.getAttrvals() == null || dto.getAttrvals().getAttrval() == null) {
      return Collections.emptyList();
    }
    return dto.getAttrvals().getAttrval().stream()
      .map(attrval -> mapAttribute(attrval, locale))
      .toList();
  }

  private Attribute mapAttribute(final Attrval attrval, final Locale locale) {
    return Attribute.builder()
      .ukey(attrval.getUkey())
      .referenceIds(Map.of("attrId", attrval.getAttrdId().longValue()))
      .references(mapAttrvalValues(attrval, locale))
      .build();
  }

  private Map<String, java.lang.Object> mapAttrvalValues(final Attrval attrval, final Locale locale) {
    Map<String, java.lang.Object> result = new HashMap<>();
    result.put("TEXT", attrval.getTextval());
    result.put("BOOLEAN", attrval.isBooleanval());
    result.put("CLTEXT", ClUtil.getAttrCltextValByLocale(attrval, locale));
    return result;
  }

  private Map<String, Long> mapMediaobjecttypeReferences(final MediaobjectDTO dto) {
    if (dto.getMediaobjecttypes() == null || dto.getMediaobjecttypes().getMediaobjecttype() == null) {
      return Collections.emptyMap();
    }
    Map<String, Long> result = new HashMap<>();
    for (Mediaobject.Mediaobjecttypes.Mediaobjecttype mot : dto.getMediaobjecttypes().getMediaobjecttype()) {
      if (mot.getId() != null && mot.getPos() != null) {
        result.put(mot.getPos().toString(), mot.getId().longValue());
      }
    }
    return result;
  }

  private List<MediaSpecifics> mapMediaSpecifics(final MediaobjectDTO dto) {
    if (dto.getLobvalues() == null || dto.getLobvalues().getLobvalue() == null) {
      return Collections.emptyList();
    }
    return dto.getLobvalues().getLobvalue().stream()
      .map(this::mapMediaSpecifics)
      .toList();
  }

  private MediaSpecifics mapMediaSpecifics(final Mediaobject.Lobvalues.Lobvalue lobvalue) {
    Lobval lobval = lobvalue.getLobval();
    String checksum = null;
    Long size = null;
    String mediaType = null;
    if (lobval != null) {
      checksum = lobval.getChksum() != null ? lobval.getChksum().getMd5() : null;
      size = lobval.getSize();
      mediaType = lobval.getMediatype();
    }
    return new MediaSpecifics(
      lobvalue.getLobtypeId() != null ? lobvalue.getLobtypeId().longValue() : null,
      lobvalue.getLobtypeUkey(),
      lobvalue.getLobtypeName(),
      checksum,
      size,
      mediaType
    );
  }
}
