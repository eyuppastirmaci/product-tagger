package com.producttagger.backend.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategorySchemaRepository extends JpaRepository<CategorySchema, Long> {

    // The active schema of a category is by convention its highest version
    Optional<CategorySchema> findTopByCategoryIdOrderByVersionDesc(Long categoryId);
}
