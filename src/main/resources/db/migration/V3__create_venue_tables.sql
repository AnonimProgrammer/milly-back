CREATE TABLE venues (
    id         UUID        NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    name       TEXT        NOT NULL,
    location   TEXT,
    status     TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_venues_updated_at
    BEFORE UPDATE ON venues
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE venue_memberships (
    id         UUID        NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id   UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    role       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_venue_memberships_user_venue UNIQUE (user_id, venue_id),
    CONSTRAINT fk_venue_memberships_venue FOREIGN KEY (venue_id) REFERENCES venues (id),
    CONSTRAINT fk_venue_memberships_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_venue_memberships_role CHECK (role IN ('MANAGER', 'WAITER'))
);

CREATE INDEX idx_venue_memberships_venue_id ON venue_memberships (venue_id);
CREATE INDEX idx_venue_memberships_user_id ON venue_memberships (user_id);
