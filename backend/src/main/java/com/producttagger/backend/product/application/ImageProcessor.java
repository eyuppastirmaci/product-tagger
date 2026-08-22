package com.producttagger.backend.product.application;

/**
 * Port for deriving the 1024px processed image and the thumbnail (both JPEG)
 * from an uploaded image.
 */
public interface ImageProcessor {

    ProcessedImages process(byte[] originalImage);

    record ProcessedImages(byte[] processed, byte[] thumbnail) {
    }
}
