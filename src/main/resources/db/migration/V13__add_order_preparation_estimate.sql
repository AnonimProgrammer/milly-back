ALTER TABLE orders
    ADD COLUMN approved_at TIMESTAMPTZ,
    ADD COLUMN estimated_preparation_minutes INTEGER,
    ADD COLUMN estimated_preparation_display TEXT;

ALTER TABLE orders
    ADD CONSTRAINT ck_orders_estimated_preparation_minutes
        CHECK (
            estimated_preparation_minutes IS NULL
                OR (estimated_preparation_minutes >= 1 AND estimated_preparation_minutes <= 480)
        );
