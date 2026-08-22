package com.producttagger.backend.product.application;

import com.producttagger.backend.product.domain.ImageVariant;
import com.producttagger.backend.product.domain.Product;
import com.producttagger.backend.product.domain.ProductRepository;
import com.producttagger.backend.product.domain.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-side orchestration for the product API: paging rules, aggregations and
 * image resolution live here so controllers stay pure delegates.
 */
@Service
public class ProductQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository products;
    private final ImageStorage imageStorage;

    public ProductQueryService(ProductRepository products, ImageStorage imageStorage) {
        this.products = products;
        this.imageStorage = imageStorage;
    }

    public Page<Product> list(List<ProductStatus> statuses, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return statuses == null || statuses.isEmpty()
                ? products.findAllWithCategory(pageRequest)
                : products.findByStatusIn(statuses, pageRequest);
    }

    public CountsView counts() {
        Map<String, Long> byStatus = products.countByStatus().stream()
                .collect(Collectors.toMap(row -> row.getStatus().name(), ProductRepository.StatusCount::getTotal));

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();

        Instant oldestPending = products
                .oldestCreatedAt(List.of(ProductStatus.PENDING_REVIEW, ProductStatus.FAILED))
                .orElse(null);

        return new CountsView(byStatus, total, oldestPending);
    }

    public Product get(UUID id) {
        return products.findByIdWithCategory(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Product getForReview(UUID id) {
        return products.findByIdForReview(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    public ImageDownload image(UUID id, String variant) {
        Product product = products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));

        String key = product.getImagePaths().pathFor(ImageVariant.from(variant));

        if (key == null) {
            throw new ProductNotFoundException(id);
        }

        return new ImageDownload(imageStorage.load(key), contentTypeOf(key));
    }

    // Only the original keeps its uploaded format; derived variants are always JPEG
    private static String contentTypeOf(String key) {
        if (key.endsWith(".png")) {
            return "image/png";
        }

        if (key.endsWith(".webp")) {
            return "image/webp";
        }

        return "image/jpeg";
    }

    public record CountsView(Map<String, Long> byStatus, long total, Instant oldestPendingCreatedAt) {
    }

    public record ImageDownload(InputStream content, String contentType) {
    }
}
