ALTER TABLE venue_memberships
    ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE venue_memberships
    DROP CONSTRAINT ck_venue_memberships_role;

UPDATE venue_memberships
SET role = 'EMPLOYEE'
WHERE role = 'WAITER';

UPDATE venue_memberships vm
SET role = 'OWNER'
FROM (
    SELECT DISTINCT ON (venue_id) id
    FROM venue_memberships
    ORDER BY venue_id, created_at ASC
) first_members
WHERE vm.id = first_members.id
  AND vm.role = 'MANAGER';

ALTER TABLE venue_memberships
    ADD CONSTRAINT ck_venue_memberships_role CHECK (role IN ('OWNER', 'MANAGER', 'EMPLOYEE'));

ALTER TABLE venue_memberships
    ADD CONSTRAINT ck_venue_memberships_status CHECK (status IN ('ACTIVE', 'INACTIVE'));
