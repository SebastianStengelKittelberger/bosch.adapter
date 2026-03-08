package de.kittelberger.bosch.adapter.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class MediaObject {

  private String name;
  private List<Attribute> attributes;
  private Map<String, Long> references;
  private List<MediaSpecifics> mediaSpecifics;
  private Long id;

}
