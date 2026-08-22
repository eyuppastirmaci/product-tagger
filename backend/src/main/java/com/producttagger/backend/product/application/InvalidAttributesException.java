package com.producttagger.backend.product.application;

/**
 * Extends IllegalArgumentException so the global handler maps it to 400.
 */
public class InvalidAttributesException extends IllegalArgumentException {

    public InvalidAttributesException(String message) {
        super(message);
    }
}
