package de.kittelberger.bosch.adapter.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SkuMetaData extends MetaData{

  public SkuMetaData(String name, Long id) {
    super(name, id);
  }
  public SkuMetaData(String name, Long id, String sku) {
    this(name, id);
    this.sku = sku;
  }

  private String sku;
}
