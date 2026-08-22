package com.producttagger.backend.catalog.api;

import java.util.List;

public record CategoryTreeResponse(
        String code,
        String nameTr,
        String nameEn,
        boolean leaf,
        List<CategoryTreeResponse> children) {
}
