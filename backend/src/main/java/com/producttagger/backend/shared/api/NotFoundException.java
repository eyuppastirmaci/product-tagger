package com.producttagger.backend.shared.api;

/**
 * Base type for "resource does not exist" errors; contexts subclass it so the
 * global handler can map them to 404 without knowing the contexts.
 */
public abstract class NotFoundException extends RuntimeException {

    protected NotFoundException(String message) {
        super(message);
    }
}
