package com.producttagger.backend.product.api;

import com.producttagger.backend.product.application.ProductQueryService;
import com.producttagger.backend.product.application.ProductUploadService;
import com.producttagger.backend.product.application.ReviewService;
import com.producttagger.backend.product.domain.Product;
import com.producttagger.backend.product.domain.ProductStatus;
import com.producttagger.backend.shared.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
class ProductController {

    private final ProductUploadService uploadService;
    private final ReviewService reviewService;
    private final ProductQueryService queryService;
    private final ProductEventsBroadcaster broadcaster;

    ProductController(ProductUploadService uploadService,
                      ReviewService reviewService,
                      ProductQueryService queryService,
                      ProductEventsBroadcaster broadcaster) {
        this.uploadService = uploadService;
        this.reviewService = reviewService;
        this.queryService = queryService;
        this.broadcaster = broadcaster;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ProductResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        Product product = uploadService.upload(file.getBytes(), file.getContentType());

        return ResponseEntity
                .created(URI.create("/api/products/" + product.getId()))
                .body(ProductResponse.from(product));
    }

    @GetMapping
    PageResponse<ProductResponse> list(@RequestParam(required = false) List<ProductStatus> status,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(queryService.list(status, page, size), ProductResponse::from);
    }

    @GetMapping("/counts")
    ProductCountsResponse counts() {
        ProductQueryService.CountsView counts = queryService.counts();

        return new ProductCountsResponse(counts.byStatus(), counts.total(), counts.oldestPendingCreatedAt());
    }

    @GetMapping("/{id}")
    ProductResponse get(@PathVariable UUID id) {
        return ProductResponse.from(queryService.get(id));
    }

    @GetMapping("/{id}/review")
    ReviewResponse review(@PathVariable UUID id) {
        return ReviewResponse.from(queryService.getForReview(id));
    }

    @GetMapping("/{id}/events")
    SseEmitter events(@PathVariable UUID id) {
        Product product = queryService.find(id);

        return broadcaster.subscribe(id, new ProductEventsBroadcaster.StatusPayload(
                product.getStatus().name(),
                product.getDescriptionTr() != null));
    }

    @GetMapping("/{id}/image")
    ResponseEntity<InputStreamResource> image(@PathVariable UUID id,
                                              @RequestParam(defaultValue = "thumbnail") String variant) {
        ProductQueryService.ImageDownload download = queryService.image(id, variant);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                // Stored images never change once written; let the browser cache them
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(new InputStreamResource(download.content()));
    }

    @PostMapping("/{id}/approve")
    ProductResponse approve(@PathVariable UUID id, @Valid @RequestBody ApproveRequest request) {
        return ProductResponse.from(reviewService.approve(id, request.categoryCode(), request.attributes()));
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
