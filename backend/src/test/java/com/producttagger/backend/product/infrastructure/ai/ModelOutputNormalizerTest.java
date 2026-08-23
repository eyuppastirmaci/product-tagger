package com.producttagger.backend.product.infrastructure.ai;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class ModelOutputNormalizerTest {

    private final ModelOutputNormalizer normalizer = new ModelOutputNormalizer();

    @Test
    void confidenceClampsOutOfRangeAndDefaultsMissingValues() {
        assertThat(normalizer.confidence(0.7)).isEqualTo(0.7);
        assertThat(normalizer.confidence(1.7)).isEqualTo(1.0);
        assertThat(normalizer.confidence(-3.0)).isEqualTo(0.0);
        assertThat(normalizer.confidence(null)).isEqualTo(0.0);
        assertThat(normalizer.confidence(Double.NaN)).isEqualTo(0.0);
    }

    @Test
    void confidencesClampEveryEntryAndDropNullKeys() {
        Map<String, Double> raw = new HashMap<>();

        raw.put("color", 1.5);
        raw.put("pattern", 0.4);
        raw.put("fit", null);
        raw.put(null, 0.9);

        assertThat(normalizer.confidences(raw)).containsOnly(
                entry("color", 1.0),
                entry("pattern", 0.4),
                entry("fit", 0.0));

        assertThat(normalizer.confidences(null)).isEmpty();
    }

    @Test
    void titlePassesShortValuesThroughTrimmed() {
        assertThat(normalizer.title("  Burgundy Solid T-shirt  ")).isEqualTo("Burgundy Solid T-shirt");
    }

    @Test
    void titleTruncatesToTheBudgetOnAWordBoundary() {
        String tooLong = "A very long generated product title that easily overflows the sixty character budget";

        String title = normalizer.title(tooLong);

        assertThat(title).hasSizeLessThanOrEqualTo(ModelOutputNormalizer.MAX_TITLE_LENGTH);
        assertThat(title).doesNotEndWith(" ");
        // Cut lands on a word boundary, not mid-word
        assertThat(tooLong).startsWith(title + " ");
    }

    @Test
    void titleWithoutSpacesIsHardCut() {
        String unbroken = "x".repeat(100);

        assertThat(normalizer.title(unbroken)).hasSize(ModelOutputNormalizer.MAX_TITLE_LENGTH);
    }

    @Test
    void titleStripsTrailingPunctuationAndMapsEmptyToNull() {
        assertThat(normalizer.title("Black T-shirt.")).isEqualTo("Black T-shirt");
        assertThat(normalizer.title("Black T-shirt!?")).isEqualTo("Black T-shirt");
        assertThat(normalizer.title(null)).isNull();
        assertThat(normalizer.title("   ")).isNull();
        assertThat(normalizer.title("...")).isNull();
    }
}
