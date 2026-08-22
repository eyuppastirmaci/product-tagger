package com.producttagger.backend.product.domain;

import com.producttagger.backend.catalog.domain.Category;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregate root of the product lifecycle; all state transitions go through
 * behavior methods and {@link TagRevision}s are created only here.
 */
@Entity
@Table(name = "products")
public class Product extends AbstractAggregateRoot<Product> implements Persistable<UUID> {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> attributes;

    @Embedded
    private ImagePaths imagePaths;

    @Column(name = "description_tr")
    private String descriptionTr;

    @Column(name = "description_en")
    private String descriptionEn;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("revisionNo asc")
    private List<TagRevision> revisions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    private boolean isNew = false;

    protected Product() {
    }

    private Product(UUID id, ImagePaths imagePaths) {
        this.id = id;
        this.status = ProductStatus.UPLOADED;
        this.imagePaths = imagePaths;
        this.isNew = true;
    }

    /**
     * Starts the lifecycle in {@code UPLOADED} status. The caller supplies the id
     * because the storage keys ("products/{id}/...") need it before creation.
     */
    public static Product upload(UUID id, String originalImagePath) {
        Product product = new Product(id, ImagePaths.ofOriginal(originalImagePath));

        product.registerEvent(new ProductUploaded(product.id));

        return product;
    }

    /**
     * Transition {@code UPLOADED -> PREPROCESSED}; registers the event that feeds
     * the tagging pipeline.
     */
    public void markPreprocessed(String processedPath, String thumbnailPath) {
        requireStatus(ProductStatus.UPLOADED);

        this.imagePaths = imagePaths.withProcessed(processedPath, thumbnailPath);

        this.status = ProductStatus.PREPROCESSED;

        registerEvent(new ProductReadyForTagging(id));
    }

    public void startTagging() {
        requireStatus(ProductStatus.PREPROCESSED);

        this.status = ProductStatus.TAGGING;
    }

    /**
     * Transition {@code TAGGING -> PENDING_REVIEW}: records the AI proposal as a
     * new revision; the product's own category/attributes only change on {@link #approve}.
     */
    public TagRevision proposeTagging(Category proposedCategory,
                                      Map<String, Object> proposedAttributes,
                                      Map<String, Object> confidences,
                                      String modelName) {
        requireStatus(ProductStatus.TAGGING);

        TagRevision revision = TagRevision.aiProposal(
                this, nextRevisionNo(), proposedCategory, proposedAttributes, confidences, modelName);

        revisions.add(revision);

        this.status = ProductStatus.PENDING_REVIEW;

        return revision;
    }

    /**
     * Transition {@code PENDING_REVIEW -> APPROVED}: records the reviewer's
     * decision and makes it the product's current category and attributes.
     */
    public TagRevision approve(Category finalCategory, Map<String, Object> finalAttributes) {
        requireStatus(ProductStatus.PENDING_REVIEW);

        // Products may only be assigned to leaf categories, never to tree nodes
        if (!finalCategory.isLeaf()) {
            throw new IllegalArgumentException(
                    "Products can only be assigned to leaf categories, got: " + finalCategory.getCode());
        }

        TagRevision revision = TagRevision.humanDecision(
                this, nextRevisionNo(), finalCategory, finalAttributes);

        revisions.add(revision);

        this.category = finalCategory;
        this.attributes = finalAttributes;
        this.status = ProductStatus.APPROVED;

        return revision;
    }

    public void markFailed() {
        if (status == ProductStatus.APPROVED) {
            throw new IllegalStateException("An approved product cannot be marked as failed");
        }

        this.status = ProductStatus.FAILED;
    }

    public void attachDescriptions(String descriptionTr, String descriptionEn) {
        requireStatus(ProductStatus.APPROVED);
        this.descriptionTr = descriptionTr;
        this.descriptionEn = descriptionEn;
    }

    private int nextRevisionNo() {
        return revisions.size() + 1;
    }

    private void requireStatus(ProductStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Expected product %s to be in status %s but was %s".formatted(id, expected, status));
        }
    }

    @Override
    public UUID getId() {
        return id;
    }

    /**
     * The id is assigned in the constructor, so without this flag Spring Data
     * would treat every fresh product as existing and issue a needless merge.
     */
    @Override
    public boolean isNew() {
        return isNew;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public Category getCategory() {
        return category;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public ImagePaths getImagePaths() {
        return imagePaths;
    }

    public String getDescriptionTr() {
        return descriptionTr;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    public List<TagRevision> getRevisions() {
        return List.copyOf(revisions);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
