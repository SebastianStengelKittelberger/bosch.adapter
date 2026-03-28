package de.kittelberger.bosch.adapter.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@AllArgsConstructor
@Data
public class Reference {
  private Long id;
  private String ukey;
  private String name;
  private Pair<String, List<AttrClass>> attrClasses;
}
