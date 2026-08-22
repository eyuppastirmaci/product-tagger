CREATE TABLE categories (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parent_id BIGINT REFERENCES categories (id),
    code      VARCHAR(64)  NOT NULL UNIQUE,
    name_tr   VARCHAR(128) NOT NULL,
    name_en   VARCHAR(128) NOT NULL,
    leaf      BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_categories_parent ON categories (parent_id);

CREATE TABLE category_schemas (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id BIGINT      NOT NULL REFERENCES categories (id),
    version     INT         NOT NULL,
    schema      JSONB       NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (category_id, version)
);

CREATE TABLE products (
    id                   UUID PRIMARY KEY,
    status               VARCHAR(32) NOT NULL,
    category_id          BIGINT REFERENCES categories (id),
    attributes           JSONB,
    original_image_path  VARCHAR(512),
    processed_image_path VARCHAR(512),
    thumbnail_path       VARCHAR(512),
    description_tr       TEXT,
    description_en       TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_attributes ON products USING GIN (attributes);
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_review_queue ON products (status, created_at)
    WHERE status IN ('PENDING_REVIEW', 'FAILED');

CREATE TABLE tag_revisions (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id            UUID        NOT NULL REFERENCES products (id),
    revision_no           INT         NOT NULL,
    source                VARCHAR(16) NOT NULL,
    model_name            VARCHAR(128),
    proposed_category_id  BIGINT REFERENCES categories (id),
    proposed_attributes   JSONB,
    confidences           JSONB,
    final_category_id     BIGINT REFERENCES categories (id),
    final_attributes      JSONB,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (product_id, revision_no)
);

CREATE INDEX idx_tag_revisions_product ON tag_revisions (product_id);
