package de.kittelberger.bosch.adapter.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MediaSpecifics {

  private Long id;
  private String ukey;
  private String name;
  private String checksum;
  private Long size;
  private String mediaType;
  private String fileName;
}
