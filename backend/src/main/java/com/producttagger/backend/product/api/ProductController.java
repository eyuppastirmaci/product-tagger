package com.producttagger.backend.product.api;

import com.producttagger.backend.product.application.ImageStorage;
import com.producttagger.backend.product.application.ProductNotFoundException;
import com.producttagger.backend.product.application.ProductUploadService;
import com.producttagger.backend.product.application.ReviewService;
import com.producttagger.backend.product.domain.ImageVariant;
import com.producttagger.backend.product.domain.Product;
import com.producttagger.backend.product.domain.ProductRepository;
import com.producttagger.backend.product.domain.ProductStatus;
import com.producttagger.backend.shared.api.PageResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
class ProductController {

    private final ProductUploadService uploadService;
    private final ReviewService reviewService;
    private final ProductRepository products;
    private final ImageStorage imageStorage;
    private final ProductEventsBroadcaster broadcaster;

    ProductController(ProductUploadService uploadService,
                      ReviewService reviewService,
                      ProductRepository products,
                      ImageStorage imageStorage,
                      ProductEventsBroadcaster broadcaster) {
        this.uploadService = uploadService;
        this.reviewService = reviewService;
        this.products = products;
        this.imageStorage = imageStorage;
        this.broadcaster = broadcaster;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ProductResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        var product = uploadService.upload(file.getBytes(), file.getContentType());

        return ResponseEntity
                .created(URI.create("/api/products/" + product.getId()))
                .body(ProductResponse.from(product));
    }

    @GetMapping
    PageResponse<ProductResponse> list(@RequestParam(required = false) List<ProductStatus> status,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Product> result = (status == null || status.isEmpty())
                ? products.findAllWithCategory(pageRequest)
                : products.findByStatusIn(status, pageRequest);

        return PageResponse.from(result, ProductResponse::from);
    }

    @GetMapping("/counts")
    ProductCountsResponse counts() {
        Map<String, Long> byStatus = products.countByStatus().stream()
                .collect(Collectors.toMap(row -> row.getStatus().name(), ProductRepository.StatusCount::getTotal));

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();

        Instant oldestPending = products
                .oldestCreatedAt(List.of(ProductStatus.PENDING_REVIEW, ProductStatus.FAILED))
                .orElse(null);

        return new ProductCountsResponse(byStatus, total, oldestPending);
    }

    @GetMapping("/{id}")
    ProductResponse get(@PathVariable UUID id) {
        return products.findByIdWithCategory(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @GetMapping("/{id}/review")
    ReviewResponse review(@PathVariable UUID id) {
        return products.findByIdForReview(id)
                .map(ReviewResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @GetMapping("/{id}/events")
    SseEmitter events(@PathVariable UUID id) {
        Product product = products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));

        return broadcaster.subscribe(id, new ProductEventsBroadcaster.StatusPayload(
                product.getStatus().name(),
                product.getDescriptionTr() != null));
    }

    @GetMapping("/{id}/image")
    ResponseEntity<InputStreamResource> image(@PathVariable UUID id,
                                              @RequestParam(defaultValue = "thumbnail") String variant) {
        Product product = products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));

        String key = product.getImagePaths().pathFor(ImageVariant.from(variant));

        if (key == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(contentTypeOf(key))
                // Stored images never change once written; let the browser cache them
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(new InputStreamResource(imageStorage.load(key)));
    }

    // Only the original keeps its uploaded format; derived variants are always JPEG
    private MediaType contentTypeOf(String key) {
        if (key.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }

        if (key.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }

        return MediaType.IMAGE_JPEG;
    }

    @PostMapping("/{id}/approve")
    ProductResponse approve(@PathVariable UUID id, @RequestBody ApproveRequest request) {
        if (request.categoryCode() == null || request.categoryCode().isBlank()) {
            throw new IllegalArgumentException("categoryCode is required");
        }

        Map<String, Object> attributes = request.attributes() == null ? Map.of() : request.attributes();

        return ProductResponse.from(reviewService.approve(id, request.categoryCode(), attributes));
    }

    @PostMapping("/{id}/reject")
    ProductResponse reject(@PathVariable UUID id) {
        return ProductResponse.from(reviewService.reject(id));
    }

    @PostMapping("/{id}/retag")
    ProductResponse retag(@PathVariable UUID id) {
        return ProductResponse.from(reviewService.retag(id));
    }

    @PatchMapping("/{id}/content")
    ProductResponse updateContent(@PathVariable UUID id, @RequestBody UpdateContentRequest request) {
        return ProductResponse.from(reviewService.updateContent(
                id, request.titleTr(), request.titleEn(), request.descriptionTr(), request.descriptionEn()));
    }

    @PostMapping("/{id}/content/regenerate")
    ProductResponse regenerateContent(@PathVariable UUID id) {
        return ProductResponse.from(reviewService.regenerateContent(id));
    }
}
