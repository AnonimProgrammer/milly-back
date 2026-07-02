DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
DROP TRIGGER IF EXISTS trg_user_auth_updated_at ON user_auth;
DROP TRIGGER IF EXISTS trg_venues_updated_at ON venues;
DROP TRIGGER IF EXISTS trg_tables_updated_at ON tables;
DROP TRIGGER IF EXISTS trg_menu_items_updated_at ON menu_items;
DROP TRIGGER IF EXISTS trg_orders_updated_at ON orders;
DROP TRIGGER IF EXISTS trg_payments_updated_at ON payments;

DROP FUNCTION IF EXISTS set_updated_at();

ALTER TABLE users ALTER COLUMN created_at DROP DEFAULT;
ALTER TABLE users ALTER COLUMN updated_at DROP DEFAULT;

ALTER TABLE user_auth ALTER COLUMN created_at DROP DEFAULT;
ALTER TABLE user_auth ALTER COLUMN updated_at DROP DEFAULT;

ALTER TABLE venues ALTER COLUMN created_at DROP DEFAULT;
ALTER TABLE venues ALTER COLUMN updated_at DROP DEFAULT;

ALTER TABLE venue_memberships ALTER COLUMN created_at DROP DEFAULT;

ALTER TABLE tables ALTER COLUMN created_at DROP DEFAULT;
ALTER TABLE tables ALTER COLUMN updated_at DROP DEFAULT;

ALTER TABLE menu_items ALTER COLUMN created_at DROP DEFAULT;
ALTER TABLE menu_items ALTER COLUMN updated_at DROP DEFAULT;

ALTER TABLE orders ALTER COLUMN created_at DROP DEFAULT;
ALTER TABLE orders ALTER COLUMN updated_at DROP DEFAULT;

ALTER TABLE order_items ALTER COLUMN created_at DROP DEFAULT;

ALTER TABLE payments ALTER COLUMN created_at DROP DEFAULT;
ALTER TABLE payments ALTER COLUMN updated_at DROP DEFAULT;
