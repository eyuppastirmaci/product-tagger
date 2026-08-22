package com.producttagger.backend.product.infrastructure.image;

import com.producttagger.backend.product.application.ImageProcessor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

@Component
class ThumbnailatorImageProcessor implements ImageProcessor {

    private static final int PROCESSED_MAX_SIZE = 1024;

    private static final int THUMBNAIL_MAX_SIZE = 256;

    @Override
    public ProcessedImages process(byte[] originalImage) {
        return new ProcessedImages(
                resizeToJpeg(originalImage, PROCESSED_MAX_SIZE),
                resizeToJpeg(originalImage, THUMBNAIL_MAX_SIZE));
    }

    /**
     * Scales the image down so its longest side fits maxSize (aspect ratio kept)
     * and re-encodes it as JPEG regardless of the source format.
     */
    private byte[] resizeToJpeg(byte[] source, int maxSize) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Thumbnails.of(new ByteArrayInputStream(source))
                    .size(maxSize, maxSize)
                    .outputFormat("jpg")
                    .toOutputStream(out);

            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to resize image", e);
        }
    }
}
