package com.producttagger.backend.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByCode(String code);

    // Root categories: entry points of the iterative descent
    List<Category> findByParentIsNull();

    // Children of a node: the options offered at each descent step
    List<Category> findByParentId(Long parentId);
}
