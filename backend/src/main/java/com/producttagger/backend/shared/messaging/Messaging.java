package com.producttagger.backend.shared.messaging;

public final class Messaging {

    public static final String EXCHANGE = "product-tagger";

    public static final String DEAD_LETTER_EXCHANGE = "product-tagger.dlx";

    private Messaging() {
    }
}
