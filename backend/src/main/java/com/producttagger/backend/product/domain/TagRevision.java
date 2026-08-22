package com.producttagger.backend.product.domain;

import com.producttagger.backend.catalog.domain.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * AI proposal or human decision in the {@link Product} aggregate; created only
 * through {@code Product}, hence the package-private factories.
 */
@Entity
@Table(name = "tag_revisions")
public class TagRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "revision_no", nullable = false)
    private int revisionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TagRevisionSource source;

    @Column(name = "model_name", length = 128)
    private String modelName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_category_id")
    private Category proposedCategory;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proposed_attributes")
    private Map<String, Object> proposedAttributes;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> confidences;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_category_id")
    private Category finalCategory;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "final_attributes")
    private Map<String, Object> finalAttributes;

    @Column(name = "approved_by", length = 128)
    private String approvedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TagRevision() {
    }

    private TagRevision(Product product, int revisionNo, TagRevisionSource source) {
        this.product = product;
        this.revisionNo = revisionNo;
        this.source = source;
    }

    static TagRevision aiProposal(Product product,
                                  int revisionNo,
                                  Category proposedCategory,
                                  Map<String, Object> proposedAttributes,
                                  Map<String, Object> confidences,
                                  String modelName) {
        TagRevision revision = new TagRevision(product, revisionNo, TagRevisionSource.AI);
        revision.proposedCategory = proposedCategory;
        revision.proposedAttributes = proposedAttributes;
        revision.confidences = confidences;
        revision.modelName = modelName;
        return revision;
    }

    static TagRevision humanDecision(Product product,
                                     int revisionNo,
                                     Category finalCategory,
                                     Map<String, Object> finalAttributes,
                                     String approvedBy) {
        TagRevision revision = new TagRevision(product, revisionNo, TagRevisionSource.HUMAN);
        revision.finalCategory = finalCategory;
        revision.finalAttributes = finalAttributes;
        revision.approvedBy = approvedBy;
        return revision;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public int getRevisionNo() {
        return revisionNo;
    }

    public TagRevisionSource getSource() {
        return source;
    }

    public String getModelName() {
        return modelName;
    }

    public Category getProposedCategory() {
        return proposedCategory;
    }

    public Map<String, Object> getProposedAttributes() {
        return proposedAttributes;
    }

    public Map<String, Object> getConfidences() {
        return confidences;
    }

    public Category getFinalCategory() {
        return finalCategory;
    }

    public Map<String, Object> getFinalAttributes() {
        return finalAttributes;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
