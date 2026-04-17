package de.kittelberger.bosch.adapter.model;

import java.util.List;
import java.util.Map;

public record Category(
  Long id,
  String ukey,
  Long parentId,
  List<String> skus,
  Map<String, List<Attribute>> attributes
) {}
