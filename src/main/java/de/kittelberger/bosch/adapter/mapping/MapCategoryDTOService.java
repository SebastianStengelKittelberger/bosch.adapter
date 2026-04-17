package de.kittelberger.bosch.adapter.mapping;

import de.kittelberger.bosch.adapter.data.LoadDataService;
import de.kittelberger.bosch.adapter.model.Attribute;
import de.kittelberger.bosch.adapter.model.Category;
import de.kittelberger.bosch.adapter.util.ClUtil;
import de.kittelberger.webexport602w.solr.api.dto.CategoryDTO;
import de.kittelberger.webexport602w.solr.api.dto.ProductDTO;
import de.kittelberger.webexport602w.solr.api.generated.Attrval;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class MapCategoryDTOService {

  private final LoadDataService loadDataService;

  public MapCategoryDTOService(LoadDataService loadDataService) {
    this.loadDataService = loadDataService;
  }

  public List<Category> map(final Locale locale) {
    Map<Long, List<String>> productIdToSkus = buildProductIdToSkusLookup();
    return loadDataService.getCategoryDTOs().stream()
      .map(dto -> mapCategory(dto, productIdToSkus, locale))
      .toList();
  }

  private Map<Long, List<String>> buildProductIdToSkusLookup() {
    return loadDataService.getAllProductDTOs().stream()
      .filter(p -> p.getId() != null && p.getSkus() != null && p.getSkus().getSku() != null)
      .collect(Collectors.toMap(
        ProductDTO::getId,
        p -> p.getSkus().getSku().stream()
          .map(de.kittelberger.webexport602w.solr.api.generated.Product.Skus.Sku::getSku)
          .filter(Objects::nonNull)
          .toList(),
        (a, b) -> a
      ));
  }

  private Category mapCategory(
    final CategoryDTO dto,
    final Map<Long, List<String>> productIdToSkus,
    final Locale locale
  ) {
    List<String> skus = resolveSkus(dto, productIdToSkus);
    Map<String, List<Attribute>> attributes = mapAttributes(dto, locale);
    return new Category(dto.getId(), dto.getUkey(), dto.getParentId(), skus, attributes);
  }

  private List<String> resolveSkus(final CategoryDTO dto, final Map<Long, List<String>> productIdToSkus) {
    if (dto.getProducts() == null || dto.getProducts().getProduct() == null) {
      return Collections.emptyList();
    }
    List<String> skus = new ArrayList<>();
    for (var p : dto.getProducts().getProduct()) {
      if (p.getId() == null) continue;
      List<String> found = productIdToSkus.get(p.getId().longValue());
      if (found != null) skus.addAll(found);
    }
    return skus;
  }

  private Map<String, List<Attribute>> mapAttributes(final CategoryDTO dto, final Locale locale) {
    if (dto.getAttrvals() == null || dto.getAttrvals().getAttrval() == null) {
      return Collections.emptyMap();
    }
    Map<String, List<Attribute>> result = new HashMap<>();
    for (Attrval attrval : dto.getAttrvals().getAttrval()) {
      String ukey = attrval.getUkey();
      if (ukey == null) continue;
      Attribute attribute = Attribute.builder()
        .ukey(ukey)
        .referenceIds(buildReferenceIds(attrval))
        .references(buildReferences(attrval, locale))
        .build();
      result.computeIfAbsent(ukey, k -> new ArrayList<>()).add(attribute);
    }
    return result;
  }

  private static Map<String, Long> buildReferenceIds(final Attrval attrval) {
    if (attrval.getAttrId() == null) return null;
    Map<String, Long> ids = new HashMap<>();
    ids.put("attrId", attrval.getAttrId().longValue());
    if (attrval.getMediaobject() != null) {
      ids.put("mediaObjectId", attrval.getMediaobject().longValue());
    }
    return ids;
  }

  private static Map<String, Object> buildReferences(final Attrval attrval, final Locale locale) {
    Map<String, Object> refs = new HashMap<>();
    refs.put("TEXT", attrval.getTextval());
    refs.put("BOOLEAN", attrval.isBooleanval());
    refs.put("CLTEXT", ClUtil.getAttrCltextValByLocale(attrval, locale));
    return refs;
  }
}
