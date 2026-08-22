package com.producttagger.backend.user.application;

import com.producttagger.backend.shared.api.UnauthorizedException;

public class InvalidRefreshTokenException extends UnauthorizedException {

    public InvalidRefreshTokenException() {
        super("Invalid or expired session");
    }
}
