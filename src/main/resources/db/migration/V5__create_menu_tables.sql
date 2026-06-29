CREATE TABLE menu_items (
    id          UUID           NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id    UUID           NOT NULL,
    name        TEXT           NOT NULL,
    description TEXT,
    price       NUMERIC(12, 2) NOT NULL,
    status      TEXT           NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_menu_items_venue FOREIGN KEY (venue_id) REFERENCES venues (id),
    CONSTRAINT ck_menu_items_status CHECK (status IN ('ACTIVE', 'DELETED'))
);

CREATE INDEX idx_menu_items_venue_id ON menu_items (venue_id);
CREATE INDEX idx_menu_items_venue_id_status ON menu_items (venue_id, status);

CREATE TRIGGER trg_menu_items_updated_at
    BEFORE UPDATE ON menu_items
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
