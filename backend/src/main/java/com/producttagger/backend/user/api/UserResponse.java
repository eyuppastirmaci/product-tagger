package com.producttagger.backend.user.api;

import com.producttagger.backend.user.domain.User;

public record UserResponse(Long id, String name, String email) {

    static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
