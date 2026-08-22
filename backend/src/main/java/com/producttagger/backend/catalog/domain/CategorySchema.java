package com.producttagger.backend.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "category_schemas")
public class CategorySchema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private int version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema", nullable = false)
    private Map<String, Object> schema;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CategorySchema() {
    }

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public int getVersion() {
        return version;
    }

    public Map<String, Object> getSchema() {
        return schema;
    }

    /**
     * Typed view of the JSONB schema; the only place that knows its raw layout.
     */
    public List<AttributeDefinition> attributeDefinitions() {
        List<AttributeDefinition> definitions = new ArrayList<>();

        if (schema.get("attributes") instanceof List<?> attributes) {
            for (Object entry : attributes) {
                if (entry instanceof Map<?, ?> attribute) {
                    definitions.add(toDefinition(attribute));
                }
            }
        }

        return List.copyOf(definitions);
    }

    private static AttributeDefinition toDefinition(Map<?, ?> attribute) {
        List<String> values = new ArrayList<>();

        if (attribute.get("values") instanceof List<?> rawValues) {
            for (Object value : rawValues) {
                if (value instanceof Map<?, ?> map && map.get("value") != null) {
                    values.add(map.get("value").toString());
                }
            }
        }

        return new AttributeDefinition(
                String.valueOf(attribute.get("key")),
                String.valueOf(attribute.get("type")),
                Boolean.TRUE.equals(attribute.get("required")),
                Boolean.TRUE.equals(attribute.get("multi")),
                List.copyOf(values));
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
