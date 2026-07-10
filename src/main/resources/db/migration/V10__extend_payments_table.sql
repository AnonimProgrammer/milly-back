ALTER TABLE payments
    ADD COLUMN provider           TEXT,
    ADD COLUMN payment_type       TEXT,
    ADD COLUMN provider_reference TEXT,
    ADD COLUMN provider_metadata  JSONB,
    ADD COLUMN failure_reason     TEXT;

ALTER TABLE payments
    ADD CONSTRAINT ck_payments_provider CHECK (provider IN ('CARD', 'APPLE', 'GOOGLE'));

ALTER TABLE payments
    ADD CONSTRAINT ck_payments_payment_type CHECK (payment_type IN ('FULL', 'CUSTOM', 'SPLIT'));

CREATE INDEX idx_payments_provider_reference ON payments (provider_reference);