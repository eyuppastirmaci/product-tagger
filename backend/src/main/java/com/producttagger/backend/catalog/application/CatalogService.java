package com.producttagger.backend.catalog.application;

import com.producttagger.backend.catalog.domain.Category;
import com.producttagger.backend.catalog.domain.CategoryRepository;
import com.producttagger.backend.catalog.domain.CategorySchema;
import com.producttagger.backend.catalog.domain.CategorySchemaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<Category> allCategories() {
        return categories.findAll();
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
}
