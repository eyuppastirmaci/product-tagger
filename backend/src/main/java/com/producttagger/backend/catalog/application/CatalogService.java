package com.producttagger.backend.catalog.application;

import com.producttagger.backend.catalog.domain.Category;
import com.producttagger.backend.catalog.domain.CategoryRepository;
import com.producttagger.backend.catalog.domain.CategorySchema;
import com.producttagger.backend.catalog.domain.CategorySchemaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read API of the catalog context; the tagging descent and the review flow
 * navigate the category tree through this service.
 */
@Service
public class CatalogService {

    private final CategoryRepository categories;
    private final CategorySchemaRepository schemas;

    public CatalogService(CategoryRepository categories, CategorySchemaRepository schemas) {
        this.categories = categories;
        this.schemas = schemas;
    }

    public List<CategoryNode> categoryTree() {
        List<Category> all = categories.findAll();

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

    /**
     * Resolves a leaf's active schema; non-leaf categories carry no schema.
     */
    @Transactional(readOnly = true)
    public CategorySchema activeLeafSchema(String code) {
        Category category = categoryByCode(code);

        if (!category.isLeaf()) {
            throw new IllegalArgumentException("Category '%s' is not a leaf category".formatted(code));
        }

        return activeSchemaOf(category.getId());
    }

    public Category categoryByCode(String code) {
        return categories.findByCode(code)
                .orElseThrow(() -> new CategoryNotFoundException(code));
    }

    public List<Category> rootCategories() {
        return categories.findByParentIsNull();
    }

    public List<Category> childrenOf(Long categoryId) {
        return categories.findByParentId(categoryId);
    }

    /**
     * The active schema is the highest version. A leaf without any schema is a
     * seed/config gap, reported as an error instead of tagging with no fields.
     */
    public CategorySchema activeSchemaOf(Long categoryId) {
        return schemas.findTopByCategoryIdOrderByVersionDesc(categoryId)
                .orElseThrow(() -> new IllegalStateException(
                        "No attribute schema defined for category " + categoryId));
    }

    private CategoryNode toNode(Category category, Map<Long, List<Category>> byParent) {
        List<CategoryNode> children = byParent.getOrDefault(category.getId(), List.of()).stream()
                .map(child -> toNode(child, byParent))
                .toList();

        return new CategoryNode(
                category.getCode(),
                category.getNameTr(),
                category.getNameEn(),
                category.isLeaf(),
                children);
    }
}
