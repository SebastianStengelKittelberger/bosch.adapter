package de.kittelberger.bosch.adapter.model;

import lombok.Builder;

import java.util.List;

/**
 * Represents a product type (Produkttyp) resolved from the XML catalogue.
 * Product types form a hierarchy (level, parentId) and carry a locale-specific name.
 * {@code objAttrs} lists the attribute definitions assigned to this product type,
 * each with their associated {@link AttrClass} assignments.
 */
@Builder
public record ProductType(
  Long id,
  String ukey,
  String name,
  Long parentId,
  Long level,
  List<ObjAttr> objAttrs
) {}
