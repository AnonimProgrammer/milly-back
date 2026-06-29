CREATE TABLE orders (
    id         UUID        NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id   UUID        NOT NULL,
    table_id   UUID        NOT NULL,
    status     TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at  TIMESTAMPTZ,
    CONSTRAINT fk_orders_venue FOREIGN KEY (venue_id) REFERENCES venues (id),
    CONSTRAINT fk_orders_table FOREIGN KEY (table_id) REFERENCES tables (id),
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CLOSED'))
);

CREATE INDEX idx_orders_venue_id ON orders (venue_id);
CREATE INDEX idx_orders_table_id ON orders (table_id);
CREATE INDEX idx_orders_venue_id_status ON orders (venue_id, status);

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE order_items (
    id           UUID           NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID           NOT NULL,
    menu_item_id UUID           NOT NULL,
    quantity     INT            NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_menu_item_id ON order_items (menu_item_id);
