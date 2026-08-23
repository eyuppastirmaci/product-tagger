package com.producttagger.backend.product.domain;

import com.producttagger.backend.catalog.domain.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ProductTest {

    @Test
    void uploadStartsInUploadedStatus() {
        UUID id = UUID.randomUUID();

        Product product = Product.upload(id, "products/x/original.jpg");

        assertThat(product.getId()).isEqualTo(id);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.UPLOADED);
        assertThat(product.isNew()).isTrue();
        assertThat(product.getImagePaths().getOriginal()).isEqualTo("products/x/original.jpg");
    }

    @Test
    void markPreprocessedSetsDerivedPaths() {
        Product product = uploaded();

        product.markPreprocessed("processed.jpg", "thumb.jpg");

        assertThat(product.getStatus()).isEqualTo(ProductStatus.PREPROCESSED);
        assertThat(product.getImagePaths().getProcessed()).isEqualTo("processed.jpg");
        assertThat(product.getImagePaths().getThumbnail()).isEqualTo("thumb.jpg");
    }

    @Test
    void markPreprocessedRejectsWrongStatus() {
        Product product = pendingReview();

        assertThatIllegalStateException()
                .isThrownBy(() -> product.markPreprocessed("p.jpg", "t.jpg"))
                .withMessageContaining("UPLOADED");
    }

    @Test
    void startTaggingRequiresPreprocessed() {
        Product product = uploaded();

        assertThatIllegalStateException().isThrownBy(product::startTagging);
    }

    @Test
    void proposeTaggingCreatesAiRevisionAndMovesToPendingReview() {
        Product product = tagging();
        Category category = leaf("tshirt");

        TagRevision revision = product.proposeTagging(
                category,
                Map.of("color", List.of("black")),
                Map.of("category.root", 0.4, "color", 0.9, "pattern", 0.7),
                "Siyah Tişört",
                "Black T-shirt",
                "test-model");

        assertThat(product.getStatus()).isEqualTo(ProductStatus.PENDING_REVIEW);
        assertThat(revision.getRevisionNo()).isEqualTo(1);
        assertThat(revision.getSource()).isEqualTo(TagRevisionSource.AI);
        assertThat(product.getTitleTr()).isEqualTo("Siyah Tişört");
        // category.* levels are excluded from the list-sorting confidence
        assertThat(product.getMinConfidence()).isEqualTo(0.7);
    }

    @Test
    void minConfidenceIgnoresNonNumbersAndHandlesNullMap() {
        Product withOddValues = tagging();

        withOddValues.proposeTagging(leaf("tshirt"),
                Map.of(), Map.of("color", "high", "pattern", 0.5), null, null, "m");

        assertThat(withOddValues.getMinConfidence()).isEqualTo(0.5);

        Product withNullMap = tagging();

        withNullMap.proposeTagging(leaf("tshirt"), Map.of(), null, null, null, "m");

        assertThat(withNullMap.getMinConfidence()).isNull();
    }

    @Test
    void approveRecordsHumanRevisionAndFinalState() {
        Product product = pendingReview();
        Category category = leaf("tshirt");
        Map<String, Object> attributes = Map.of("color", List.of("black"), "pattern", "solid");

        TagRevision revision = product.approve(category, attributes, "Reviewer");

        assertThat(product.getStatus()).isEqualTo(ProductStatus.APPROVED);
        assertThat(product.getCategory()).isSameAs(category);
        assertThat(product.getAttributes()).isEqualTo(attributes);
        assertThat(revision.getRevisionNo()).isEqualTo(2);
        assertThat(revision.getSource()).isEqualTo(TagRevisionSource.HUMAN);
        assertThat(revision.getApprovedBy()).isEqualTo("Reviewer");
        assertThat(product.getRevisions()).hasSize(2);
    }

    @Test
    void approveRejectsNonLeafCategory() {
        Product product = pendingReview();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> product.approve(node("clothing"), Map.of(), "Reviewer"))
                .withMessageContaining("leaf");
    }

    @Test
    void approveRejectsWrongStatus() {
        Product product = uploaded();

        assertThatIllegalStateException()
                .isThrownBy(() -> product.approve(leaf("tshirt"), Map.of(), "Reviewer"));
    }

    @Test
    void rejectOnlyFromPendingReview() {
        Product product = pendingReview();

        product.reject();

        assertThat(product.getStatus()).isEqualTo(ProductStatus.REJECTED);

        assertThatIllegalStateException().isThrownBy(product::reject);
    }

    @Test
    void retaggingReentersPipelineFromReviewableStates() {
        Product pending = pendingReview();

        pending.requestRetagging();

        assertThat(pending.getStatus()).isEqualTo(ProductStatus.PREPROCESSED);

        Product rejected = pendingReview();

        rejected.reject();
        rejected.requestRetagging();

        assertThat(rejected.getStatus()).isEqualTo(ProductStatus.PREPROCESSED);

        Product failed = tagging();

        failed.markFailed();
        failed.requestRetagging();

        assertThat(failed.getStatus()).isEqualTo(ProductStatus.PREPROCESSED);
    }

    @Test
    void approvedProductCannotBeRetaggedOrFailed() {
        Product product = approved();

        assertThatIllegalStateException().isThrownBy(product::requestRetagging);
        assertThatIllegalStateException().isThrownBy(product::markFailed);
    }

    @Test
    void generatedContentLifecycleRequiresApproved() {
        Product product = approved();

        product.attachGeneratedContent("t-tr", "t-en", "d-tr", "d-en");

        assertThat(product.getDescriptionTr()).isEqualTo("d-tr");

        product.requestContentRegeneration();

        assertThat(product.getTitleTr()).isNull();
        assertThat(product.getDescriptionTr()).isNull();

        Product pending = pendingReview();

        assertThatIllegalStateException()
                .isThrownBy(() -> pending.attachGeneratedContent("t", "t", "d", "d"));
    }

    @Test
    void eachRetagCycleIncrementsRevisionNumbers() {
        Product product = pendingReview();

        product.requestRetagging();
        product.startTagging();

        TagRevision second = product.proposeTagging(leaf("jeans"), Map.of(), Map.of(), null, null, "m");

        assertThat(second.getRevisionNo()).isEqualTo(2);
        assertThat(product.getRevisions()).hasSize(2);
    }

    private static Product uploaded() {
        return Product.upload(UUID.randomUUID(), "products/x/original.jpg");
    }

    private static Product tagging() {
        Product product = uploaded();

        product.markPreprocessed("processed.jpg", "thumb.jpg");
        product.startTagging();

        return product;
    }

    private static Product pendingReview() {
        Product product = tagging();

        product.proposeTagging(leaf("tshirt"), Map.of(), Map.of("color", 0.9), "t", "t", "test-model");

        return product;
    }

    private static Product approved() {
        Product product = pendingReview();

        product.approve(leaf("tshirt"), Map.of("color", List.of("black")), "Reviewer");

        return product;
    }

    private static Category leaf(String code) {
        return category(code, true);
    }

    private static Category node(String code) {
        return category(code, false);
    }

    // Category has no public constructor by design; tests build it reflectively
    private static Category category(String code, boolean leaf) {
        Category category = BeanUtils.instantiateClass(Category.class);

        ReflectionTestUtils.setField(category, "id", 1L);
        ReflectionTestUtils.setField(category, "code", code);
        ReflectionTestUtils.setField(category, "leaf", leaf);

        return category;
    }
}
