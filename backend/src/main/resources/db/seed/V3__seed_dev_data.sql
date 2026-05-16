INSERT INTO tenants (id, name, slug)
VALUES ('00000000-0000-0000-0000-000000000001', 'Dev Org', 'dev-org')
ON CONFLICT (slug) DO NOTHING;

-- IMPORTANT: Replace password_hash values with output from GenerateDevHashes.java
-- Run: src/test/java/com/vanhdev/backend/tools/GenerateDevHashes.java
INSERT INTO users (id, tenant_id, email, password_hash, role)
VALUES
    ('00000000-0000-0000-0000-000000000010',
     '00000000-0000-0000-0000-000000000001',
     'admin@devorg.local',
     '$2a$10$REPLACE_WITH_BCRYPT_HASH_OF_Admin@123_______________________',
     'ADMIN'),
    ('00000000-0000-0000-0000-000000000011',
     '00000000-0000-0000-0000-000000000001',
     'user@devorg.local',
     '$2a$10$REPLACE_WITH_BCRYPT_HASH_OF_User@123________________________',
     'USER')
ON CONFLICT (tenant_id, email) DO NOTHING;