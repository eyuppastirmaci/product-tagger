package com.producttagger.backend.product.api;

import com.producttagger.backend.product.application.ProductNotFoundException;
import com.producttagger.backend.product.application.ProductUploadService;
import com.producttagger.backend.product.domain.ProductRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
class ProductController {

    private final ProductUploadService uploadService;

    private final ProductRepository products;

    ProductController(ProductUploadService uploadService, ProductRepository products) {
        this.uploadService = uploadService;
        this.products = products;
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

    @GetMapping("/{id}")
    ProductResponse get(@PathVariable UUID id) {
        return products.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
