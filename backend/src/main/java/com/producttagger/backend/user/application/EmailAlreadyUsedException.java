package com.producttagger.backend.user.application;

/**
 * Extends IllegalStateException so the global handler maps it to 409.
 */
public class EmailAlreadyUsedException extends IllegalStateException {

    public EmailAlreadyUsedException() {
        super("This email is already registered");
    }
}
