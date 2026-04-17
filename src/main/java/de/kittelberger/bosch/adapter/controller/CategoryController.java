package de.kittelberger.bosch.adapter.controller;

import de.kittelberger.bosch.adapter.data.ElasticsearchCategoryIndexService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class CategoryController {

  private final ElasticsearchCategoryIndexService elasticsearchCategoryIndexService;

  public CategoryController(ElasticsearchCategoryIndexService elasticsearchCategoryIndexService) {
    this.elasticsearchCategoryIndexService = elasticsearchCategoryIndexService;
  }

  /**
   * Indexes all categories for the given locale into Elasticsearch.
   */
  @GetMapping("/{country}/{language}/index/categories")
  public ResponseEntity<String> indexCategories(
    @PathVariable String country,
    @PathVariable String language
  ) {
    elasticsearchCategoryIndexService.indexCategories(country, language);
    return ResponseEntity.ok("It worked");
  }
}
