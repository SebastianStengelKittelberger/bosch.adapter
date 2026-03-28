package de.kittelberger.bosch.adapter.mapping;

import de.kittelberger.bosch.adapter.data.LoadDataService;
import de.kittelberger.bosch.adapter.model.AttrClass;
import de.kittelberger.bosch.adapter.model.Reference;
import de.kittelberger.bosch.adapter.util.ClUtil;
import de.kittelberger.webexport602w.solr.api.dto.AttrDTO;
import de.kittelberger.webexport602w.solr.api.generated.Attr;
import de.kittelberger.webexport602w.solr.api.generated.Attrclass;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Component
public class MapAttrDTOService {

  private final LoadDataService loadDataService;

  public MapAttrDTOService(LoadDataService loadDataService) {
    this.loadDataService = loadDataService;
  }

  public List<Reference> map(final Locale locale) {
    return loadDataService.getAttrDTOs().stream()
      .map(attrDTO -> map(attrDTO, locale))
      .toList();
  }

  private Reference map(final AttrDTO attrDTO, final Locale locale) {
    return new Reference(
      attrDTO.getId(),
      attrDTO.getUkey(),
      ClUtil.getValue(attrDTO.getName(), locale),
      Pair.of(attrDTO.getUkey(), mapAttrClasses(attrDTO.getAttrclasses(), locale))
    );
  }

  private List<AttrClass> mapAttrClasses(final Attr.Attrclasses attrclasses, final Locale locale) {
    if (attrclasses == null || attrclasses.getAttrclass() == null) {
      return Collections.emptyList();
    }
    return attrclasses.getAttrclass().stream()
      .map(attrclass -> mapAttrClass(attrclass, locale))
      .toList();
  }

  private AttrClass mapAttrClass(final Attrclass attrclass, final Locale locale) {
    String name = ClUtil.getValue(attrclass.getName(), locale);
    return new AttrClass(name, attrclass.getUkey());
  }
}
