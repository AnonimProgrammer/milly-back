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

CREATE TABLE roles (
    id   UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE user_auth (
    id               UUID        NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID        NOT NULL,
    provider_id      UUID        NOT NULL,
    provider_user_id TEXT,
    email            TEXT,
    password_hash    TEXT,
    metadata         JSONB,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_auth_provider_email UNIQUE (provider_id, email),
    CONSTRAINT uk_user_auth_provider_user UNIQUE (provider_id, provider_user_id),
    CONSTRAINT fk_user_auth_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_auth_provider FOREIGN KEY (provider_id) REFERENCES auth_providers (id)
);
