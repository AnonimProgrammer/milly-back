CREATE TABLE payments (
    id         UUID           NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id   UUID           NOT NULL,
    amount     NUMERIC(12, 2) NOT NULL,
    status     TEXT           NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT ck_payments_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_payments_order_id ON payments (order_id);

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
