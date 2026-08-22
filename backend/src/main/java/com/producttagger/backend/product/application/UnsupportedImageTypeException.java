package com.producttagger.backend.product.application;

/**
 * Extends IllegalArgumentException so the global handler maps it to 400.
 */
public class UnsupportedImageTypeException extends IllegalArgumentException {

    public UnsupportedImageTypeException(String contentType) {
        super("Unsupported image type: " + contentType);
    }
}
