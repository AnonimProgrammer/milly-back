INSERT INTO auth_providers (id, type)
VALUES
    (gen_random_uuid(), 'PASSWORD'),
    (gen_random_uuid(), 'GOOGLE'),
    (gen_random_uuid(), 'APPLE');

INSERT INTO roles (id, name)
VALUES
    (gen_random_uuid(), 'USER'),
    (gen_random_uuid(), 'ADMIN');
