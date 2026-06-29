CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE auth_providers (
    id         UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    type       TEXT NOT NULL,
    CONSTRAINT uk_auth_providers_type UNIQUE (type)
);

CREATE TABLE users (
    id           UUID        NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name   TEXT        NOT NULL,
    last_name    TEXT        NOT NULL,
    email        TEXT        NOT NULL,
    phone_number TEXT,
    birth_date   DATE        NOT NULL,
    status       TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
