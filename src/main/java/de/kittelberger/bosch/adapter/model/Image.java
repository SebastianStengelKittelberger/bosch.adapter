package de.kittelberger.bosch.adapter.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Image {
  private String fileName;
  private String url;
  private String ukey;
  private Long size;
  private String seoImageId;
  private ImageDimension imageDimension;
  private Double aspectRatio;
  private String title;
  private String altText;
  private Long mediaItemId;
  private Long mediaObjectId;
}
