package de.kittelberger.bosch.adapter.data;

import de.kittelberger.webexport602w.solr.api.dto.*;
import de.kittelberger.webexport602w.solr.api.generated.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

import static de.kittelberger.bosch.adapter.data.XmlConversionUtil.*;


@Service
public class LoadDataService {

  private final XmlFileLoader xmlFileLoader;

  @Value("${xml.product-limit:-1}")
  private int productLimit;

  @Value("${xml.product-ids:}")
  private String configuredProductIds;

  public LoadDataService(XmlFileLoader xmlFileLoader) {
    this.xmlFileLoader = xmlFileLoader;
  }

  // ---------------------------------------------------------------------------
  // Public methods – one per XML type
  // ---------------------------------------------------------------------------

  public List<ProductDTO> getProductDTOs() {
    Set<Long> requestedProductIds = parseConfiguredProductIds();
    List<ProductDTO> result = new ArrayList<>();
    xmlFileLoader.forEachElementOfType("product", "product", Product.class, product -> {
      Long productId = toLong(product.getId());
      if (!requestedProductIds.isEmpty() && !requestedProductIds.contains(productId)) {
        return true;
      }

      result.add(toProductDTO(product));
      return productLimit < 0 || result.size() < productLimit;
    });
    return result;
  }

  public List<SkuDTO> getSkuDTOs(Set<Long> productIds) {
    List<SkuDTO> result = new ArrayList<>();
    if (productIds.isEmpty()) {
      return result;
    }

    xmlFileLoader.forEachElementOfType("sku", "sku", Sku.class, sku -> {
      Long productId = toLong(sku.getProduct());
      if (!productIds.contains(productId)) {
        return true;
      }

      result.add(toSkuDTO(sku));
      return true;
    });
    return result;
  }

  @Cacheable("attrs")
  public List<AttrDTO> getAttrDTOs() {
    List<AttrDTO> result = new ArrayList<>();
    xmlFileLoader.forEachFileOfType("attr", we -> {
      Webexport.BusinessObjects bo = we.getBusinessObjects();
      if (bo == null) {
        return;
      }
      for (Attr a : bo.getAttr()) {
        AttrDTO dto = new AttrDTO();
        dto.setId(toLong(a.getId()));
        dto.setCdat(toDate(a.getCdat()));
        dto.setUdat(toDate(a.getUdat()));
        dto.setAction(toChar(a.getAction()));
        dto.setDdat(toDate(a.getDdat()));
        dto.setUkey(a.getUkey());
        dto.setAttrclasses(a.getAttrclasses());
        dto.setName(toMap(a.getName()));
        dto.setShortname(toMap(a.getShortname()));
        dto.setLongname(toMap(a.getLongname()));
        dto.setDescr(toMap(a.getDescr()));
        dto.setDatatype(a.getDatatype());
        dto.setValues(a.getValues());
        dto.setUoms(a.getUoms());
        dto.setCols(a.getCols());
        dto.setTranslatable(a.isTranslatable());
        dto.setProducttypes(a.getProducttypes());
        dto.setMediaobjecttypes(a.getMediaobjecttypes());
        dto.setRefobjecttypes(a.getRefobjecttypes());
        result.add(dto);
      }
    });
    return result;
  }

  @Cacheable("categories")
  public List<CategoryDTO> getCategoryDTOs() {
    List<CategoryDTO> result = new ArrayList<>();
    xmlFileLoader.forEachFileOfType("category", we -> {
      Webexport.BusinessObjects bo = we.getBusinessObjects();
      if (bo == null) {
        return;
      }
      for (Category c : bo.getCategory()) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(toLong(c.getId()));
        dto.setCdat(toDate(c.getCdat()));
        dto.setUdat(toDate(c.getUdat()));
        dto.setAction(toChar(c.getAction()));
        dto.setDdat(toDate(c.getDdat()));
        dto.setParentId(toLong(c.getParentId()));
        dto.setLevel(toLong(c.getLevel()));
        dto.setCtypeId(toLong(c.getCtypeId()));
        dto.setCtypeUkey(c.getCtypeUkey());
        dto.setUkey(c.getUkey());
        dto.setName(toMap(c.getName()));
        dto.setPos(toLong(c.getPos()));
        dto.setShortdesc(toMap(c.getShortdesc()));
        dto.setLongdesc(toMap(c.getLongdesc()));
        dto.setProducts(c.getProducts());
        dto.setRefobjects(c.getRefobjects());
        dto.setCatalogtables(c.getCatalogtables());
        dto.setAttrvals(c.getAttrvals());
        if (c.getMediaobjects() != null) {
          dto.setMediaobjects_mediaobject_id(
            c.getMediaobjects().getMediaobject().stream()
              .map(m -> toLong(m.getId()))
              .filter(Objects::nonNull)
              .toArray(Long[]::new));
        }
        result.add(dto);
      }
    });
    return result;
  }

  @Cacheable("categorytypes")
  public List<CategorytypeDTO> getCategorytypeDTOs() {
    List<CategorytypeDTO> result = new ArrayList<>();
    xmlFileLoader.forEachFileOfType("categorytype", we -> {
      Webexport.BusinessObjects bo = we.getBusinessObjects();
      if (bo == null) {
        return;
      }
      for (Categorytype ct : bo.getCategorytype()) {
        CategorytypeDTO dto = new CategorytypeDTO();
        dto.setId(toLong(ct.getId()));
        dto.setCdat(toDate(ct.getCdat()));
        dto.setUdat(toDate(ct.getUdat()));
        dto.setAction(toChar(ct.getAction()));
        dto.setLevel(toLong(ct.getLevel()));
        dto.setUkey(ct.getUkey());
        dto.setName(toMap(ct.getName()));
        dto.setPos(ct.getPos() != null ? ct.getPos().toString() : null);
        dto.setShortdesc(toMap(ct.getShortdesc()));
        dto.setLongdesc(toMap(ct.getLongdesc()));
        dto.setCatattrs(ct.getCatattrs());
        result.add(dto);
      }
    });
    return result;
  }

  @Cacheable("lobtypes")
  public List<LobtypeDTO> getLobtypeDTOs() {
    List<LobtypeDTO> result = new ArrayList<>();
    xmlFileLoader.forEachFileOfType("lobtype", we -> {
      Webexport.BusinessObjects bo = we.getBusinessObjects();
      if (bo == null) {
        return;
      }
      for (Lobtype lt : bo.getLobtype()) {
        LobtypeDTO dto = new LobtypeDTO();
        dto.setId(toLong(lt.getId()));
        dto.setCdat(toDate(lt.getCdat()));
        dto.setUdat(toDate(lt.getUdat()));
        dto.setAction(toChar(lt.getAction()));
        dto.setDdat(toDate(lt.getDdat()));
        dto.setMediaobjecttypeId(toLong(lt.getMediaobjecttypeId()));
        dto.setMediaobjecttypeUkey(lt.getMediaobjecttypeUkey());
        dto.setPos(lt.getPos() != null ? Long.parseLong(lt.getPos()) : null);
        dto.setMediatype(lt.getMediatype());
        dto.setAttrclasses(lt.getAttrclasses());
        dto.setUkey(lt.getUkey());
        dto.setName(toMap(lt.getName()));
        dto.setDescr(toMap(lt.getDescr()));
        result.add(dto);
      }
    });
    return result;
  }

  @Cacheable("mediaobjects")
  public List<MediaobjectDTO> getMediaobjectDTOs() {
    List<MediaobjectDTO> result = new ArrayList<>();
    xmlFileLoader.forEachFileOfType("mediaobject", we -> {
      Webexport.BusinessObjects bo = we.getBusinessObjects();
      if (bo == null) {
        return;
      }
      for (Mediaobject m : bo.getMediaobject()) {
        MediaobjectDTO dto = new MediaobjectDTO();
        dto.setId(toLong(m.getId()));
        dto.setCdat(toDate(m.getCdat()));
        dto.setUdat(toDate(m.getUdat()));
        dto.setAction(toChar(m.getAction()));
        dto.setDdat(toDate(m.getDdat()));
        dto.setName(toMap(m.getName()));
        dto.setText(toMap(m.getText()));
        dto.setCategories(m.getCategories());
        dto.setAttrvals(m.getAttrvals());
        dto.setImg(m.getImg());
        dto.setMediaobjecttypes(m.getMediaobjecttypes());
        dto.setLobvalues(m.getLobvalues());
        result.add(dto);
      }
    });
    return result;
  }

  @Cacheable("mediaobjecttypes")
  public List<MediaobjecttypeDTO> getMediaobjecttypeDTOs() {
    List<MediaobjecttypeDTO> result = new ArrayList<>();
    xmlFileLoader.forEachFileOfType("mediaobjecttype", we -> {
      Webexport.BusinessObjects bo = we.getBusinessObjects();
      if (bo == null) {
        return;
      }
      for (Mediaobjecttype mt : bo.getMediaobjecttype()) {
        MediaobjecttypeDTO dto = new MediaobjecttypeDTO();
        dto.setId(toLong(mt.getId()));
        dto.setCdat(toDate(mt.getCdat()));
        dto.setUdat(toDate(mt.getUdat()));
        dto.setAction(toChar(mt.getAction()));
        dto.setDdat(toDate(mt.getDdat()));
        dto.setParentId(toLong(mt.getParentId()));
        dto.setLevel(toLong(mt.getLevel()));
        dto.setUkey(mt.getUkey());
        dto.setName(toMap(mt.getName()));
        dto.setPos(mt.getPos() != null ? mt.getPos().toString() : null);
        dto.setShortdesc(toMap(mt.getShortdesc()));
        dto.setLongdesc(toMap(mt.getLongdesc()));
        dto.setAttrvals(mt.getAttrvals());
        dto.setObjattrs(mt.getObjattrs());
        dto.setMediaobjects(mt.getMediaobjects());
        result.add(dto);
      }
    });
    return result;
  }

  @Cacheable("mfacts")
  public List<MfactDTO> getMfactDTOs() {
    List<MfactDTO> result = new ArrayList<>();
    xmlFileLoader.forEachFileOfType("mfact", we -> {
      Webexport.BusinessObjects bo = we.getBusinessObjects();
      if (bo == null) {
        return;
      }
      for (Mfact m : bo.getMfact()) {
        MfactDTO dto = new MfactDTO();
        dto.setId(toLong(m.getId()));
        dto.setCdat(toDate(m.getCdat()));
        dto.setUdat(toDate(m.getUdat()));
        dto.setAction(toChar(m.getAction()));
        dto.setDdat(toDate(m.getDdat()));
        dto.setUkey(m.getUkey());
        dto.setName(m.getName());
        Lobval logo = m.getLogo();
        if (logo != null) {
          dto.setLogo_fname(logo.getFname());
          dto.setLogo_mediatype(logo.getMediatype());
          dto.setLogo_size(logo.getSize());
          dto.setLogo_date(toDate(logo.getDate()));
          if (logo.getChksum() != null) {
            dto.setLogo_chksum_md4(logo.getChksum().getMd4());
            dto.setLogo_chksum_md5(logo.getChksum().getMd5());
            dto.setLogo_chksum_sh1(logo.getChksum().getSh1());
          }
        }
        result.add(dto);
      }
    });
    return result;
  }

  @Cacheable("prices")
  public List<PriceDTO> getPriceDTOs() {
    List<PriceDTO> result = new ArrayList<>();
    xmlFileLoader.forEachFileOfType("price", we -> {
      Webexport.BusinessObjects bo = we.getBusinessObjects();
      if (bo == null) {
        return;
      }
      for (Price p : bo.getPrice()) {
        PriceDTO dto = new PriceDTO();
        dto.setCdat(toDate(p.getCdat()));
        dto.setUdat(toDate(p.getUdat()));
        dto.setAction(toChar(p.getAction()));
        dto.setDdat(toDate(p.getDdat()));
        dto.setSkuId(toLong(p.getSkuId()));
        dto.setPricetypeId(toLong(p.getPricetypeId()));
        dto.setCurrency(p.getCurrency());
        dto.setMfact(p.getMfact());
        dto.setArtno(p.getArtno());
        dto.setCso(p.getCso());
        dto.setSku(p.getSku());
        dto.setDiscountGroup(p.getDiscountGroup());
        dto.setGtin(p.getGtin());
        dto.setEan(p.getEan());
        dto.setUpc(p.getUpc());
        dto.setOrderUnit(p.getOrderUnit());
        dto.setQuantity(p.getQuantity() != null ? p.getQuantity().longValue() : null);
        dto.setMinOrderAmount(p.getMinOrderAmount() != null ? p.getMinOrderAmount().longValue() : null);
        dto.setContentUnit(p.getContentUnit());
        dto.setContentPerOrderUnit(p.getContentPerOrderUnit() != null ? p.getContentPerOrderUnit().longValue() : null);
        dto.setPricetypeName(toMap(p.getPricetypeName()));
        dto.setPricetypeDescr(toMap(p.getPricetypeDescr()));
        dto.setPriceValue(p.getPriceValue() != null ? p.getPriceValue().doubleValue() : null);
        dto.setPriceDescr(toMap(p.getPriceDescr()));
        dto.setValidFrom(toDate(p.getValidFrom()));
        dto.setValidTo(toDate(p.getValidTo()));
        result.add(dto);
      }
    });
    return result;
  }

  @Cacheable("producttypes")
  public List<ProducttypeDTO> getProducttypeDTOs() {
    List<ProducttypeDTO> result = new ArrayList<>();
    xmlFileLoader.forEachFileOfType("producttype", we -> {
      Webexport.BusinessObjects bo = we.getBusinessObjects();
      if (bo == null) {
        return;
      }
      for (Producttype pt : bo.getProducttype()) {
        ProducttypeDTO dto = new ProducttypeDTO();
        dto.setId(toLong(pt.getId()));
        dto.setCdat(toDate(pt.getCdat()));
        dto.setUdat(toDate(pt.getUdat()));
        dto.setAction(toChar(pt.getAction()));
        dto.setDdat(toDate(pt.getDdat()));
        dto.setParentId(toLong(pt.getParentId()));
        dto.setLevel(toLong(pt.getLevel()));
        dto.setUkey(pt.getUkey());
        dto.setName(toMap(pt.getName()));
        dto.setPos(pt.getPos() != null ? pt.getPos().toString() : null);
        dto.setShortdesc(toMap(pt.getShortdesc()));
        dto.setLongdesc(toMap(pt.getLongdesc()));
        dto.setAttrvals(pt.getAttrvals());
        dto.setObjattrs(pt.getObjattrs());
        dto.setSkuattrs(pt.getSkuattrs());
        dto.setProducts(pt.getProducts());
        result.add(dto);
      }
    });
    return result;
  }

  private Set<Long> parseConfiguredProductIds() {
    if (configuredProductIds == null || configuredProductIds.isBlank()) {
      return Set.of();
    }

    Set<Long> productIds = new LinkedHashSet<>();
    for (String token : configuredProductIds.split(",")) {
      String trimmed = token.trim();
      if (!trimmed.isEmpty()) {
        productIds.add(Long.valueOf(trimmed));
      }
    }
    return productIds;
  }

  private ProductDTO toProductDTO(Product p) {
    ProductDTO dto = new ProductDTO();
    dto.setId(toLong(p.getId()));
    dto.setCdat(toDate(p.getCdat()));
    dto.setUdat(toDate(p.getUdat()));
    dto.setAction(toChar(p.getAction()));
    dto.setDdat(toDate(p.getDdat()));
    dto.setMfact(p.getMfact());
    dto.setArtno(p.getArtno());
    dto.setName(toMap(p.getName()));
    dto.setText(toMap(p.getText()));
    dto.setAttrvals(p.getAttrvals());
    dto.setLogo(p.getLogo());
    dto.setImg(p.getImg());
    dto.setSkus(p.getSkus());
    if (p.getCategories() != null) {
      dto.setCategories_category_id(
        p.getCategories().getCategory().stream()
          .map(c -> toLong(c.getId()))
          .filter(Objects::nonNull)
          .toArray(Long[]::new));
    }
    if (p.getProducttypes() != null) {
      dto.setProducttypes_producttype_id(
        p.getProducttypes().getProducttype().stream()
          .map(pt -> toLong(pt.getId()))
          .filter(Objects::nonNull)
          .toArray(Long[]::new));
    }
    return dto;
  }

  private SkuDTO toSkuDTO(Sku s) {
    SkuDTO dto = new SkuDTO();
    dto.setId(toLong(s.getId()));
    dto.setCdat(toDate(s.getCdat()));
    dto.setUdat(toDate(s.getUdat()));
    dto.setAction(toChar(s.getAction()));
    dto.setDdat(toDate(s.getDdat()));
    dto.setProduct(toLong(s.getProduct()));
    dto.setMfact(s.getMfact());
    dto.setArtno(s.getArtno());
    dto.setCso(s.getCso());
    dto.setSku(s.getSku());
    dto.setGtin(s.getGtin());
    dto.setEan(s.getEan());
    dto.setUpc(s.getUpc());
    dto.setAttrvals(s.getAttrvals());
    return dto;
  }
}
