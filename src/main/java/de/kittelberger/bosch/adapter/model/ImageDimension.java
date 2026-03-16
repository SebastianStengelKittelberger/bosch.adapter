package de.kittelberger.bosch.adapter.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@AllArgsConstructor
@Data
public class ImageDimension implements Serializable {
  private static final long serialVersionUID = -8458720648151102522L;
  private String width;
  private String height;
}
