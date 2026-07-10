ALTER TABLE payments
    ADD COLUMN tip_amount NUMERIC(12, 2) NOT NULL DEFAULT 0;

ALTER TABLE payments
    ADD CONSTRAINT ck_payments_tip_amount_nonneg CHECK (tip_amount >= 0);
