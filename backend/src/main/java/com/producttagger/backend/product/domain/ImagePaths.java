package com.producttagger.backend.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ImagePaths {

    @Column(name = "original_image_path", length = 512)
    private String original;

    @Column(name = "processed_image_path", length = 512)
    private String processed;

    @Column(name = "thumbnail_path", length = 512)
    private String thumbnail;

    protected ImagePaths() {
    }

    private ImagePaths(String original, String processed, String thumbnail) {
        this.original = original;
        this.processed = processed;
        this.thumbnail = thumbnail;
    }

    public static ImagePaths ofOriginal(String original) {
        return new ImagePaths(original, null, null);
    }

    public ImagePaths withProcessed(String processed, String thumbnail) {
        return new ImagePaths(original, processed, thumbnail);
    }

    // Exhaustive switch: adding a variant without a path is a compile error
    public String pathFor(ImageVariant variant) {
        return switch (variant) {
            case ORIGINAL -> original;
            case PROCESSED -> processed;
            case THUMBNAIL -> thumbnail;
        };
    }

    public String getOriginal() {
        return original;
    }

    public String getProcessed() {
        return processed;
    }

    public String getThumbnail() {
        return thumbnail;
    }
}
