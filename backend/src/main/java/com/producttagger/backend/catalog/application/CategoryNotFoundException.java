package com.producttagger.backend.catalog.application;

import com.producttagger.backend.shared.api.NotFoundException;

public class CategoryNotFoundException extends NotFoundException {

    public CategoryNotFoundException(String code) {
        super("Category '%s' not found".formatted(code));
    }
}
