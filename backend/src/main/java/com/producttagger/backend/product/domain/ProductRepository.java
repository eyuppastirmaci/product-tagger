package com.producttagger.backend.product.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    // For responses mapped outside a transaction: the category must be fetched
    // eagerly or an approved product hits LazyInitializationException
    @Query("select p from Product p left join fetch p.category where p.id = :id")
    Optional<Product> findByIdWithCategory(UUID id);

    // Review queue and other status listings; backed by the partial index on
    // (status, created_at)
    @EntityGraph(attributePaths = "category")
    Page<Product> findByStatusIn(Collection<ProductStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("select p from Product p")
    Page<Product> findAllWithCategory(Pageable pageable);

    // Everything the review screen needs, eagerly fetched so mapping can happen
    // outside a transaction (open-in-view is off)
    @Query("""
            select distinct p from Product p
            left join fetch p.revisions r
            left join fetch r.proposedCategory
            left join fetch r.finalCategory
            left join fetch p.category
            where p.id = :id
            """)
    Optional<Product> findByIdForReview(UUID id);
}
