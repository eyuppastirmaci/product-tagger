package com.producttagger.backend.shared.api;

/**
 * Base type for authentication failures; contexts subclass it so the global
 * handler can map them to 401 without knowing the contexts.
 */
public abstract class UnauthorizedException extends RuntimeException {

    protected UnauthorizedException(String message) {
        super(message);
    }
}
