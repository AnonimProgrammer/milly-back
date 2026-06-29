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
