ALTER TABLE menu_items
    ADD COLUMN approximate_preparation_minutes INTEGER NOT NULL DEFAULT 15;

ALTER TABLE menu_items
    ADD CONSTRAINT ck_menu_items_approximate_preparation_minutes
        CHECK (approximate_preparation_minutes >= 1 AND approximate_preparation_minutes <= 480);
