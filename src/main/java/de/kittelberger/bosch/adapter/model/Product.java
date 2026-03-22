package de.kittelberger.bosch.adapter.model;

import java.util.List;
import java.util.Map;

public record Product(
  ProductMetaData productMetaData,
  List<SkuMetaData> skuMetaData,
  List<Attribute> productAttributes,
  Map<String, List<Attribute>> skuAttributes,
  List<ProductType> productTypes
) {}
