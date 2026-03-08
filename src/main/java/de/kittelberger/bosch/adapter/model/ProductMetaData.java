package de.kittelberger.bosch.adapter.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProductMetaData extends MetaData
{
  public ProductMetaData(String name, Long id) {
    super(name, id);
  }

  public ProductMetaData(String name, Long id, String artNo) {
    this(name, id);
    this.artNo = artNo;
  }

  private String artNo;
}
