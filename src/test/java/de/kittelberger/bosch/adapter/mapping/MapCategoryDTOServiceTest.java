package de.kittelberger.bosch.adapter.mapping;

import de.kittelberger.bosch.adapter.data.LoadDataService;
import de.kittelberger.bosch.adapter.model.Category;
import de.kittelberger.webexport602w.solr.api.dto.CategoryDTO;
import de.kittelberger.webexport602w.solr.api.dto.ProductDTO;
import de.kittelberger.webexport602w.solr.api.generated.Attrval;
import de.kittelberger.webexport602w.solr.api.generated.Category.Products;
import de.kittelberger.webexport602w.solr.api.generated.Category.Products.Product;
import de.kittelberger.webexport602w.solr.api.generated.Product.Skus;
import de.kittelberger.webexport602w.solr.api.generated.Product.Skus.Sku;
import de.kittelberger.webexport602w.solr.api.generated.Val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MapCategoryDTOServiceTest {

  @Mock
  private LoadDataService loadDataService;

  private MapCategoryDTOService service;

  @BeforeEach
  void setUp() {
    service = new MapCategoryDTOService(loadDataService);
  }

  @Test
  void map_withNoCategoryDTOs_returnsEmptyList() {
    when(loadDataService.getCategoryDTOs()).thenReturn(List.of());
    when(loadDataService.getAllProductDTOs()).thenReturn(List.of());

    assertThat(service.map(Locale.GERMAN)).isEmpty();
  }

  @Test
  void map_resolvesSkusFromContainedProductIds() {
    ProductDTO product = productDTO(42L, List.of("SKU-001", "SKU-002"));
    CategoryDTO category = categoryDTO(1L, "CAT-001", null, List.of(productRef(42L)), null);

    when(loadDataService.getAllProductDTOs()).thenReturn(List.of(product));
    when(loadDataService.getCategoryDTOs()).thenReturn(List.of(category));

    List<Category> result = service.map(Locale.GERMAN);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).skus()).containsExactlyInAnyOrder("SKU-001", "SKU-002");
  }

  @Test
  void map_withUnknownProductId_producesEmptySkuList() {
    CategoryDTO category = categoryDTO(1L, "CAT-001", null, List.of(productRef(99L)), null);

    when(loadDataService.getAllProductDTOs()).thenReturn(List.of());
    when(loadDataService.getCategoryDTOs()).thenReturn(List.of(category));

    List<Category> result = service.map(Locale.GERMAN);

    assertThat(result.get(0).skus()).isEmpty();
  }

  @Test
  void map_withNoProducts_producesEmptySkuList() {
    CategoryDTO category = categoryDTO(1L, "CAT-001", null, null, null);

    when(loadDataService.getAllProductDTOs()).thenReturn(List.of());
    when(loadDataService.getCategoryDTOs()).thenReturn(List.of(category));

    List<Category> result = service.map(Locale.GERMAN);

    assertThat(result.get(0).skus()).isEmpty();
  }

  @Test
  void map_mapsTextAttrvalToAttribute() {
    Attrval attrval = new Attrval();
    attrval.setUkey("TITLE");
    attrval.setTextval("Elektrowerkzeuge");
    attrval.setBooleanval(false);

    CategoryDTO category = categoryDTO(1L, "CAT-001", null, null, attrvals(attrval));

    when(loadDataService.getAllProductDTOs()).thenReturn(List.of());
    when(loadDataService.getCategoryDTOs()).thenReturn(List.of(category));

    List<Category> result = service.map(Locale.GERMAN);

    assertThat(result.get(0).attributes()).containsKey("TITLE");
    assertThat(result.get(0).attributes().get("TITLE")).hasSize(1);
    assertThat(result.get(0).attributes().get("TITLE").get(0).getReferences().get("TEXT"))
      .isEqualTo("Elektrowerkzeuge");
  }

  @Test
  void map_setsIdUkeyAndParentId() {
    CategoryDTO category = categoryDTO(5L, "CAT-005", 3L, null, null);

    when(loadDataService.getAllProductDTOs()).thenReturn(List.of());
    when(loadDataService.getCategoryDTOs()).thenReturn(List.of(category));

    Category result = service.map(Locale.GERMAN).get(0);

    assertThat(result.id()).isEqualTo(5L);
    assertThat(result.ukey()).isEqualTo("CAT-005");
    assertThat(result.parentId()).isEqualTo(3L);
  }

  @Test
  void map_aggregatesSkusAcrossMultipleProducts() {
    ProductDTO p1 = productDTO(1L, List.of("A-001"));
    ProductDTO p2 = productDTO(2L, List.of("B-001", "B-002"));
    CategoryDTO category = categoryDTO(10L, "CAT-010", null,
      List.of(productRef(1L), productRef(2L)), null);

    when(loadDataService.getAllProductDTOs()).thenReturn(List.of(p1, p2));
    when(loadDataService.getCategoryDTOs()).thenReturn(List.of(category));

    Category result = service.map(Locale.GERMAN).get(0);

    assertThat(result.skus()).containsExactlyInAnyOrder("A-001", "B-001", "B-002");
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static ProductDTO productDTO(long id, List<String> skuCodes) {
    ProductDTO dto = new ProductDTO();
    dto.setId(id);
    Skus skus = new Skus();
    for (String code : skuCodes) {
      Sku sku = new Sku();
      sku.setSku(code);
      skus.getSku().add(sku);
    }
    dto.setSkus(skus);
    return dto;
  }

  private static Product productRef(long id) {
    Product p = new Product();
    p.setId(BigInteger.valueOf(id));
    return p;
  }

  private static CategoryDTO categoryDTO(
    Long id, String ukey, Long parentId,
    List<Product> productRefs, Val attrvals
  ) {
    CategoryDTO dto = new CategoryDTO();
    dto.setId(id);
    dto.setUkey(ukey);
    dto.setParentId(parentId);
    if (productRefs != null) {
      Products products = new Products();
      products.getProduct().addAll(productRefs);
      dto.setProducts(products);
    }
    dto.setAttrvals(attrvals);
    return dto;
  }

  private static Val attrvals(Attrval... attrvalList) {
    Val val = new Val();
    val.getAttrval().addAll(List.of(attrvalList));
    return val;
  }
}
