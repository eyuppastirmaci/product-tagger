package com.producttagger.backend.product.application;

import com.producttagger.backend.shared.api.NotFoundException;

import java.util.UUID;

public class ProductNotFoundException extends NotFoundException {

    public ProductNotFoundException(UUID productId) {
        super("Product %s not found".formatted(productId));
    }
}
