package com.producttagger.backend.catalog.api;

import com.producttagger.backend.catalog.application.CatalogService;
import com.producttagger.backend.catalog.application.SchemaSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
class CategoryController {

    private final CatalogService catalog;

    CategoryController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    List<CategoryTreeResponse> tree() {
        return catalog.categoryTree().stream()
                .map(CategoryTreeResponse::from)
                .toList();
    }

    @GetMapping("/{code}/schema")
    CategorySchemaResponse schema(@PathVariable String code) {
        SchemaSnapshot schema = catalog.activeLeafSchema(code);

        return new CategorySchemaResponse(schema.categoryCode(), schema.version(), schema.schema());
    }
}
