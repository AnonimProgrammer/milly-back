CREATE TABLE tables (
    id         UUID        NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id   UUID        NOT NULL,
    label      TEXT        NOT NULL,
    status     TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_tables_venue FOREIGN KEY (venue_id) REFERENCES venues (id),
    CONSTRAINT ck_tables_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_tables_venue_id ON tables (venue_id);
CREATE INDEX idx_tables_venue_id_status ON tables (venue_id, status);

CREATE TRIGGER trg_tables_updated_at
    BEFORE UPDATE ON tables
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
