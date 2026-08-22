package com.producttagger.backend.product.api;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record ApproveRequest(@NotBlank String categoryCode, Map<String, Object> attributes) {
}
