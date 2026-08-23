package com.producttagger.backend.product.application;

import com.producttagger.backend.IntegrationTest;
import com.producttagger.backend.product.domain.Product;
import com.producttagger.backend.product.domain.ProductRepository;
import com.producttagger.backend.product.domain.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ReviewConcurrencyTest extends IntegrationTest {

    private static final Map<String, Object> VALID_TSHIRT_ATTRIBUTES = Map.of(
            "color", List.of("black"),
            "pattern", "solid",
            "sleeve_length", "short");

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ProductRepository products;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * Approve and reject racing on the same product: optimistic locking must
     * let exactly one decision through and fail the other, never merge both.
     */
    @Test
    void concurrentApproveAndRejectYieldExactlyOneDecision() throws Exception {
        UUID productId = pendingReviewProduct();

        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Object> approve = () -> {
            barrier.await();

            try {
                return reviewService.approve(productId, "tshirt", VALID_TSHIRT_ATTRIBUTES);
            } catch (OptimisticLockingFailureException | IllegalStateException e) {
                return e;
            }
        };
        Callable<Object> reject = () -> {
            barrier.await();

            try {
                return reviewService.reject(productId);
            } catch (OptimisticLockingFailureException | IllegalStateException e) {
                return e;
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Object>> results;
        try {
            results = pool.invokeAll(List.of(approve, reject));
        } finally {
            pool.shutdown();
        }

        long decisions = 0;
        long conflicts = 0;
        for (Future<Object> result : results) {
            if (result.get() instanceof Product) {
                decisions++;
            } else if (result.get() instanceof Exception) {
                conflicts++;
            }
        }

        assertThat(decisions).isEqualTo(1);
        assertThat(conflicts).isEqualTo(1);

        // The surviving state must be internally consistent with the winner
        inTransaction(() -> {
            Product product = products.findById(productId).orElseThrow();

            if (product.getStatus() == ProductStatus.APPROVED) {
                assertThat(product.getCategory().getCode()).isEqualTo("tshirt");
                assertThat(product.getAttributes()).isEqualTo(VALID_TSHIRT_ATTRIBUTES);
                assertThat(product.getRevisions()).hasSize(2);
            } else {
                assertThat(product.getStatus()).isEqualTo(ProductStatus.REJECTED);
                assertThat(product.getCategory()).isNull();
                assertThat(product.getRevisions()).hasSize(1);
            }

            return null;
        });
    }

    @Test
    void secondApproveOfTheSameProductIsRejected() {
        UUID productId = pendingReviewProduct();

        reviewService.approve(productId, "tshirt", VALID_TSHIRT_ATTRIBUTES);

        // Sequential double-approve hits the state guard, not the version check
        assertThatIllegalStateException()
                .isThrownBy(() -> reviewService.approve(productId, "tshirt", VALID_TSHIRT_ATTRIBUTES));
    }

    // Drives a fresh product to PENDING_REVIEW; the registered pipeline events
    // are published but every consumer skips a product that is already past
    // the tagging stages
    private UUID pendingReviewProduct() {
        UUID id = UUID.randomUUID();
        Product product = Product.upload(id, "products/%s/original.jpg".formatted(id));

        product.markPreprocessed("products/%s/processed.jpg".formatted(id), "products/%s/thumbnail.jpg".formatted(id));
        product.startTagging();
        product.proposeTagging(null, null, Map.of("category.root", 0.2), null, null, "test-model");

        products.save(product);

        return id;
    }

    private void inTransaction(Callable<Void> work) {
        new TransactionTemplate(transactionManager).execute(tx -> {
            try {
                return work.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
