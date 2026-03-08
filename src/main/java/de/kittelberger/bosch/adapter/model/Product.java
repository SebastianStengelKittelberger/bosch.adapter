package de.kittelberger.bosch.adapter.model;

import java.util.List;

public record Product(
  ProductMetaData productMetaData,
  List<SkuMetaData> skuMetaData,
  List<Attribute> productAttributes,
  List<Attribute> skuAttributes
){

}
