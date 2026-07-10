ALTER TABLE menu_items
    ADD COLUMN category TEXT NOT NULL DEFAULT 'MAINS';

ALTER TABLE menu_items
    ADD CONSTRAINT ck_menu_items_category CHECK (category IN ('STARTERS', 'MAINS', 'DRINKS', 'DESSERTS'));
