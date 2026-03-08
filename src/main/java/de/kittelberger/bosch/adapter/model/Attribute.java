package de.kittelberger.bosch.adapter.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Builder
@Data
public class Attribute {

  private String ukey;
  private Map<String, Long> referenceIds;
  /**
   * key: UKEY, value: Map of type und value
   */
  private Map<String, Object> references;

}
