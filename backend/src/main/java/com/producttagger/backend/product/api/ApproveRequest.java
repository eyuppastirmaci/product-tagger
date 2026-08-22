package com.producttagger.backend.product.api;

import java.util.Map;

public record ApproveRequest(String categoryCode, Map<String, Object> attributes) {
}
