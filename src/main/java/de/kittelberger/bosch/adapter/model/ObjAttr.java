package de.kittelberger.bosch.adapter.model;

import java.util.List;

/**
 * Represents an attribute definition from a producttype's {@code objattrs} section.
 * Each {@code ObjAttr} groups its {@link AttrClass} assignments — the set of attribute
 * classes that determine which products or SKUs the attribute is relevant for.
 */
public record ObjAttr(
  String ukey,
  String name,
  Long attrId,
  List<AttrClass> attrClasses
) {}
