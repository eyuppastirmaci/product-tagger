package com.producttagger.backend.user.application;

import com.producttagger.backend.shared.api.TooManyRequestsException;

public class LoginRateLimitException extends TooManyRequestsException {

    public LoginRateLimitException() {
        super("Too many login attempts; try again later");
    }
}
