package com.producttagger.backend.user.application;

import com.producttagger.backend.shared.api.UnauthorizedException;

public class InvalidCredentialsException extends UnauthorizedException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
