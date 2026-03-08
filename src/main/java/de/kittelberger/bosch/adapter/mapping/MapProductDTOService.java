package de.kittelberger.bosch.adapter.mapping;

import de.kittelberger.bosch.adapter.data.LoadDataService;
import de.kittelberger.bosch.adapter.model.Attribute;
import de.kittelberger.bosch.adapter.model.Product;
import de.kittelberger.bosch.adapter.model.ProductMetaData;
import de.kittelberger.bosch.adapter.model.SkuMetaData;
import de.kittelberger.bosch.adapter.util.ClUtil;
import de.kittelberger.webexport602w.solr.api.dto.ProductDTO;
import de.kittelberger.webexport602w.solr.api.dto.SkuDTO;
import de.kittelberger.webexport602w.solr.api.generated.Attrval;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MapProductDTOService {

  private final LoadDataService loadDataService;

  public MapProductDTOService(LoadDataService loadDataService) {
    this.loadDataService = loadDataService;
  }

  public List<Product> map(final Locale locale) {
    List<ProductDTO> productDTOs = loadDataService.getProductDTOs();
    List<SkuDTO> skuDTOs = loadDataService.getSkuDTOs();
    return productDTOs.stream().map(productDTO -> {
      List<String> skus = productDTO.getSkus().getSku()
        .stream()
        .map(de.kittelberger.webexport602w.solr.api.generated.Product.Skus.Sku::getSku)
        .toList();
      List<SkuDTO> skusForProduct = skuDTOs.stream()
        .filter(skuDTO -> skus.stream().anyMatch(skuDTO.getSku()::equals))
        .toList();
      return map(productDTO, skusForProduct, locale);
    })
      .toList();
  }

  private Product map(
    final ProductDTO productDTO,
    final List<SkuDTO> skuDTO,
    final Locale locale
  ) {
    return new Product(
      mapProductMetaData(productDTO, locale),
      mapSkuMetaData(skuDTO, locale),
      mapProductAttributes(productDTO, locale),
      skuDTO.stream().map(sku -> mapSkuAttributes(sku, locale)).flatMap(Collection::stream).toList()
    );
  }


  private ProductMetaData mapProductMetaData(
    final ProductDTO productDTO,
    final Locale locale
  ){
    return new ProductMetaData(ClUtil.getCleanedValue(productDTO.getName(), locale), productDTO.getId(), productDTO.getArtno());
  }

  private List<SkuMetaData> mapSkuMetaData(
    final List<SkuDTO> skuDTO,
    final Locale locale
  ){
    return skuDTO
      .stream()
      .map(sku -> new SkuMetaData(ClUtil.getCleanedValue(sku.getName(), locale), sku.getId(), sku.getArtno()))
      .toList();
  }

  private List<Attribute> mapProductAttributes(
    final ProductDTO productDTO,
    final Locale locale) {
    List<Attrval> attrvals = productDTO.getAttrvals().getAttrval();
    return attrvals.stream().map(attrval ->
        Attribute.builder()
          .ukey(attrval.getUkey())
          .referenceIds(Map.of(
            "attrId", attrval.getAttrdId().longValue()
          ))
          .references(Pair.of(
            attrval.getUkey(), mapAttrvalValues(attrval, locale))
          )
          .build())
      .toList();

  }

  private List<Attribute> mapSkuAttributes(
    final SkuDTO skuDTO,
    final Locale locale
  ) {
    List<Attrval> attrvals = skuDTO.getAttrvals().getAttrval();
    return attrvals.stream().map(attrval ->
        Attribute.builder()
          .ukey(attrval.getUkey())
          .referenceIds(Map.of(
            "attrId", attrval.getAttrdId().longValue()
          ))
          .references(Pair.of(
            attrval.getUkey(), mapAttrvalValues(attrval, locale))
          )
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
