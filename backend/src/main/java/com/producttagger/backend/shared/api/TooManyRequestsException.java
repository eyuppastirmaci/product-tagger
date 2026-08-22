package com.producttagger.backend.shared.api;

/**
 * Base type for rate-limit violations; contexts subclass it so the global
 * handler can map them to 429 without knowing the contexts.
 */
public abstract class TooManyRequestsException extends RuntimeException {

    protected TooManyRequestsException(String message) {
        super(message);
    }
}
