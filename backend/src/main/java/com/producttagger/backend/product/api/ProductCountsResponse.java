package com.producttagger.backend.product.api;

import java.time.Instant;
import java.util.Map;

public record ProductCountsResponse(
        Map<String, Long> byStatus,
        long total,
        Instant oldestPendingCreatedAt) {
}
