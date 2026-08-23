package com.producttagger.backend.product.application;

import com.producttagger.backend.product.domain.Product;
import com.producttagger.backend.product.domain.ProductRepository;
import com.producttagger.backend.product.domain.ProductStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductUploadServiceTest {

    private final RecordingImageStorage storage = new RecordingImageStorage();
    private final ProductRepository products = mock(ProductRepository.class);
    private final ProductUploadService service = new ProductUploadService(
            storage, new StubImageProcessor(), products, noopTransactionManager());

    @Test
    void happyPathStoresAllVariantsAndSavesPreprocessedProduct() {
        when(products.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Product product = service.upload(new byte[]{1, 2, 3}, MediaType.IMAGE_JPEG_VALUE);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.PREPROCESSED);
        assertThat(storage.storedKeys).containsExactly(
                "products/%s/original.jpg".formatted(product.getId()),
                "products/%s/processed.jpg".formatted(product.getId()),
                "products/%s/thumbnail.jpg".formatted(product.getId()));
        assertThat(storage.deletedKeys).isEmpty();
    }

    @Test
    void rejectsEmptyFileAndUnknownContentType() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.upload(new byte[0], MediaType.IMAGE_JPEG_VALUE));

        assertThatExceptionOfType(UnsupportedImageTypeException.class)
                .isThrownBy(() -> service.upload(new byte[]{1}, "application/pdf"));

        assertThat(storage.storedKeys).isEmpty();
    }

    @Test
    void failedStoreDeletesTheAlreadyStoredObjects() {
        storage.failOnKeyContaining = "processed";

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> service.upload(new byte[]{1, 2, 3}, MediaType.IMAGE_JPEG_VALUE))
                .withMessage("storage down");

        // Only the original made it in, and the compensation removed it again
        assertThat(storage.storedKeys).hasSize(1);
        assertThat(storage.deletedKeys).containsExactlyElementsOf(storage.storedKeys);
    }

    @Test
    void failedSaveDeletesEveryStoredObject() {
        when(products.save(any())).thenThrow(new RuntimeException("db down"));

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> service.upload(new byte[]{1, 2, 3}, MediaType.IMAGE_JPEG_VALUE))
                .withMessage("db down");

        assertThat(storage.storedKeys).hasSize(3);
        assertThat(storage.deletedKeys).containsExactlyElementsOf(storage.storedKeys);
    }

    @Test
    void cleanupFailuresDoNotMaskTheOriginalError() {
        when(products.save(any())).thenThrow(new RuntimeException("db down"));
        storage.failDeletes = true;

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> service.upload(new byte[]{1, 2, 3}, MediaType.IMAGE_JPEG_VALUE))
                .withMessage("db down");
    }

    @Test
    void processingFailureAbortsBeforeAnyStorageWrite() {
        ImageProcessor failingProcessor = content -> {
            throw new IllegalStateException("cannot decode");
        };
        ProductUploadService failingService = new ProductUploadService(
                storage, failingProcessor, products, noopTransactionManager());

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> failingService.upload(new byte[]{1}, MediaType.IMAGE_JPEG_VALUE));

        assertThat(storage.storedKeys).isEmpty();
        Mockito.verifyNoInteractions(products);
    }

    // The template needs a real status object; the manager itself is a no-op
    private static PlatformTransactionManager noopTransactionManager() {
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);

        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        return manager;
    }

    private static final class RecordingImageStorage implements ImageStorage {

        private final List<String> storedKeys = new ArrayList<>();
        private final List<String> deletedKeys = new ArrayList<>();
        private String failOnKeyContaining;
        private boolean failDeletes;

        @Override
        public void store(String key, InputStream content, long contentLength, String contentType) {
            if (failOnKeyContaining != null && key.contains(failOnKeyContaining)) {
                throw new IllegalStateException("storage down");
            }

            storedKeys.add(key);
        }

        @Override
        public InputStream load(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(String key) {
            if (failDeletes) {
                throw new IllegalStateException("delete failed");
            }

            deletedKeys.add(key);
        }
    }

    private static final class StubImageProcessor implements ImageProcessor {

        @Override
        public ProcessedImages process(byte[] content) {
            return new ProcessedImages(new byte[]{9}, new byte[]{8});
        }
    }
}
