package com.producttagger.backend.product.domain;

public enum ImageVariant {
    ORIGINAL,
    PROCESSED,
    THUMBNAIL;

    /**
     * Case-insensitive parse so URLs can use lowercase ("?variant=thumbnail").
     */
    public static ImageVariant from(String raw) {
        for (ImageVariant variant : values()) {
            if (variant.name().equalsIgnoreCase(raw)) {
                return variant;
            }
        }

        throw new IllegalArgumentException("Unknown image variant: " + raw);
    }
}
