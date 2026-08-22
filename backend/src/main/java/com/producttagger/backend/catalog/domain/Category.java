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

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "name_tr", nullable = false, length = 128)
    private String nameTr;

    @Column(name = "name_en", nullable = false, length = 128)
    private String nameEn;

    @Column(nullable = false)
    private boolean leaf;

    protected Category() {
    }

    public Long getId() {
        return id;
    }

    public Category getParent() {
        return parent;
    }

    public String getCode() {
        return code;
    }

    public String getNameTr() {
        return nameTr;
    }

    public String getNameEn() {
        return nameEn;
    }

    public boolean isLeaf() {
        return leaf;
    }
}
