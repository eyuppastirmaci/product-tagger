package com.producttagger.backend.catalog.api;

import com.producttagger.backend.catalog.application.CatalogService;
import com.producttagger.backend.catalog.domain.Category;
import com.producttagger.backend.catalog.domain.CategorySchema;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
class CategoryController {

    private final CatalogService catalog;

    CategoryController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    List<CategoryTreeResponse> tree() {
        List<Category> all = catalog.allCategories();

        // Group children by parent id; accessing only getId() on the lazy parent
        // proxy is safe because it never triggers initialization
        Map<Long, List<Category>> byParent = all.stream()
                .filter(category -> category.getParent() != null)
                .collect(Collectors.groupingBy(category -> category.getParent().getId()));

        return all.stream()
                .filter(category -> category.getParent() == null)
                .map(root -> toNode(root, byParent))
                .toList();
    }

    @GetMapping("/{code}/schema")
    CategorySchemaResponse schema(@PathVariable String code) {
        Category category = catalog.categoryByCode(code);

        // Only leaves carry attribute schemas
        if (!category.isLeaf()) {
            throw new IllegalArgumentException("Category '%s' is not a leaf category".formatted(code));
        }

        CategorySchema schema = catalog.activeSchemaOf(category.getId());

        return new CategorySchemaResponse(category.getCode(), schema.getVersion(), schema.getSchema());
    }

    private CategoryTreeResponse toNode(Category category, Map<Long, List<Category>> byParent) {
        List<CategoryTreeResponse> children = byParent.getOrDefault(category.getId(), List.of()).stream()
                .map(child -> toNode(child, byParent))
                .toList();

        return new CategoryTreeResponse(
                category.getCode(),
                category.getNameTr(),
                category.getNameEn(),
                category.isLeaf(),
                children);
    }
}
