package de.kittelberger.bosch.adapter.mapping;

import de.kittelberger.bosch.adapter.data.LoadDataService;
import de.kittelberger.bosch.adapter.model.AttrClass;
import de.kittelberger.bosch.adapter.model.Attribute;
import de.kittelberger.bosch.adapter.model.ObjAttr;
import de.kittelberger.bosch.adapter.model.Product;
import de.kittelberger.bosch.adapter.model.ProductMetaData;
import de.kittelberger.bosch.adapter.model.ProductType;
import de.kittelberger.bosch.adapter.model.SkuMetaData;
import de.kittelberger.bosch.adapter.util.ClUtil;
import de.kittelberger.webexport602w.solr.api.dto.ProductDTO;
import de.kittelberger.webexport602w.solr.api.dto.ProducttypeDTO;
import de.kittelberger.webexport602w.solr.api.dto.SkuDTO;
import de.kittelberger.webexport602w.solr.api.generated.Attrclass;
import de.kittelberger.webexport602w.solr.api.generated.Attrval;
import de.kittelberger.webexport602w.solr.api.generated.Objecttype;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
public class MapProductDTOService {

  private final LoadDataService loadDataService;

  public MapProductDTOService(LoadDataService loadDataService) {
    this.loadDataService = loadDataService;
  }

  public List<Product> map(final Locale locale) {
    List<Product> products = new ArrayList<>();
    stream(locale, products::add);
    return products;
  }

  public void stream(final Locale locale, Consumer<Product> consumer) {
    List<ProductDTO> productDTOs = new ArrayList<>(loadDataService.getProductDTOs());
    Set<Long> requestedProductIds = productDTOs.stream()
      .map(ProductDTO::getId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    Set<String> requestedSkuCodes = productDTOs.stream()
      .filter(productDTO -> productDTO.getSkus() != null && productDTO.getSkus().getSku() != null)
      .flatMap(productDTO -> productDTO.getSkus().getSku().stream())
      .map(de.kittelberger.webexport602w.solr.api.generated.Product.Skus.Sku::getSku)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    List<SkuDTO> skuDTOs = new ArrayList<>(loadDataService.getSkuDTOs(requestedProductIds));
    Map<String, List<SkuDTO>> skuDTOsBySku = skuDTOs.stream()
      .filter(skuDTO -> skuDTO.getSku() != null && requestedSkuCodes.contains(skuDTO.getSku()))
      .collect(Collectors.groupingBy(SkuDTO::getSku));

    Map<Long, ProducttypeDTO> productTypeById = loadDataService.getProducttypeDTOs().stream()
      .filter(pt -> pt.getId() != null)
      .collect(Collectors.toMap(ProducttypeDTO::getId, pt -> pt, (a, b) -> a));

    try {
      for (ProductDTO productDTO : productDTOs) {
        Product product = mapProduct(productDTO, skuDTOsBySku, productTypeById, locale);
        if (product != null) {
          consumer.accept(product);
        }
      }
    } finally {
      productDTOs.clear();
      skuDTOs.clear();
      skuDTOsBySku.clear();
    }
  }

  private Product mapProduct(
    final ProductDTO productDTO,
    final Map<String, List<SkuDTO>> skuDTOsBySku,
    final Map<Long, ProducttypeDTO> productTypeById,
    final Locale locale
  ) {
    if (productDTO.getSkus() == null || productDTO.getSkus().getSku() == null) {
      return null;
    }

    List<SkuDTO> skusForProduct = productDTO.getSkus().getSku()
      .stream()
      .map(de.kittelberger.webexport602w.solr.api.generated.Product.Skus.Sku::getSku)
      .filter(Objects::nonNull)
      .map(skuDTOsBySku::get)
      .filter(Objects::nonNull)
      .flatMap(Collection::stream)
      .toList();

    Map<String, List<Attribute>> skuAttributes = skusForProduct.stream()
      .filter(skuDTO -> skuDTO.getAttrvals() != null && skuDTO.getAttrvals().getAttrval() != null)
      .collect(Collectors.toMap(
        SkuDTO::getSku,
        skuDTO -> mapSkuAttributes(skuDTO, locale)
      ));

    return new Product(
      mapProductMetaData(productDTO, locale),
      mapSkuMetaData(skusForProduct, locale),
      mapProductAttributes(productDTO, locale),
      skuAttributes,
      mapProductTypes(productDTO, productTypeById, locale)
    );
  }

  private List<ProductType> mapProductTypes(
    final ProductDTO productDTO,
    final Map<Long, ProducttypeDTO> productTypeById,
    final Locale locale
  ) {
    Long[] ids = productDTO.getProducttypes_producttype_id();
    if (ids == null || ids.length == 0) {
      return List.of();
    }
    return Arrays.stream(ids)
      .filter(Objects::nonNull)
      .map(productTypeById::get)
      .filter(Objects::nonNull)
      .map(pt -> new ProductType(
        pt.getId(),
        pt.getUkey(),
        ClUtil.getCleanedValue(pt.getName(), locale),
        pt.getParentId(),
        pt.getLevel(),
        mapObjAttrs(pt.getObjattrs(), locale)
      ))
      .toList();
  }

  private List<ObjAttr> mapObjAttrs(
    final Objecttype.Objattrs objattrs,
    final Locale locale
  ) {
    if (objattrs == null || objattrs.getAttr() == null) {
      return List.of();
    }
    return objattrs.getAttr().stream()
      .map(attr -> new ObjAttr(
        attr.getUkey(),
        attr.getName(),
        attr.getAttrId() != null ? attr.getAttrId().longValue() : null,
        mapAttrClasses(attr.getAttrclasses(), locale)
      ))
      .toList();
  }

  private List<AttrClass> mapAttrClasses(
    final Objecttype.Objattrs.Attr.Attrclasses attrclasses,
    final Locale locale
  ) {
    if (attrclasses == null || attrclasses.getAttrclass() == null) {
      return List.of();
    }
    return attrclasses.getAttrclass().stream()
      .map(ac -> new AttrClass(
        ClUtil.getValue(ac.getName(), locale),
        ac.getUkey()
      ))
      .toList();
  }

  private ProductMetaData mapProductMetaData(
    final ProductDTO productDTO,
    final Locale locale
  ) {
    return new ProductMetaData(ClUtil.getCleanedValue(productDTO.getName(), locale), productDTO.getId(), productDTO.getArtno());
  }

  private List<SkuMetaData> mapSkuMetaData(
    final List<SkuDTO> skuDTO,
    final Locale locale
  ) {
    return skuDTO
      .stream()
      .map(sku -> new SkuMetaData(ClUtil.getCleanedValue(sku.getName(), locale), sku.getId(), sku.getArtno()))
      .toList();
  }

  private List<Attribute> mapProductAttributes(
    final ProductDTO productDTO,
    final Locale locale
  ) {
    List<Attrval> attrvals = productDTO.getAttrvals().getAttrval();
    return attrvals.stream().map(attrval ->
        Attribute.builder()
          .ukey(attrval.getUkey())
          .referenceIds(mapProductReferenceIds(attrval))
          .references(mapAttrvalValues(attrval, locale))
          .build())
      .toList();
  }

  private Map<String, Long> mapProductReferenceIds(final Attrval attrval) {
    if (attrval.getAttrId() == null) {
      return null;
    }
    Map<String, Long> result = new HashMap<>();
    result.put("attrId", attrval.getAttrId().longValue());
    if (attrval.getMediaobject() != null) {
      result.put("mediaObjectId", attrval.getMediaobject().longValue());
    }
    return result;
  }

  private List<Attribute> mapSkuAttributes(
    final SkuDTO skuDTO,
    final Locale locale
  ) {
    List<Attrval> attrvals = skuDTO.getAttrvals().getAttrval();
    return attrvals.stream().map(attrval ->
        Attribute.builder()
          .ukey(attrval.getUkey())
          .referenceIds(mapProductReferenceIds(attrval))
          .references(mapAttrvalValues(attrval, locale))
          .build())
      .toList();
  }

  private Map<String, Object> mapAttrvalValues(Attrval attrval, Locale locale) {
    Map<String, Object> result = new HashMap<>();
    result.put("TEXT", attrval.getTextval());
    result.put("BOOLEAN", attrval.isBooleanval());
    result.put("CLTEXT", ClUtil.getAttrCltextValByLocale(attrval, locale));
    return result;
  }
}
