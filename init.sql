-- ============================================================
-- テーブル生成 DDL
-- ============================================================

CREATE TABLE tenants (
                         id          BIGSERIAL    PRIMARY KEY,
                         code        VARCHAR(50)  NOT NULL UNIQUE,
                         name        VARCHAR(100) NOT NULL,
                         is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
                         created_at  TIMESTAMPTZ  NOT NULL,
                         updated_at  TIMESTAMPTZ  NOT NULL,
                         created_by  VARCHAR      NOT NULL,
                         updated_by  VARCHAR      NOT NULL
);

CREATE TABLE users (
                       id            BIGSERIAL    PRIMARY KEY,
                       tenant_id     BIGINT       NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
                       email         VARCHAR(254) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role          VARCHAR(20)  NOT NULL,
                       is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at    TIMESTAMPTZ  NOT NULL,
                       updated_at    TIMESTAMPTZ  NOT NULL,
                       created_by    VARCHAR      NOT NULL,
                       updated_by    VARCHAR      NOT NULL,
                       CONSTRAINT users_tenant_id_email_key UNIQUE (tenant_id, email)
);

CREATE TABLE refresh_tokens (
                                id         BIGSERIAL    PRIMARY KEY,
                                tenant_id  BIGINT       NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
                                user_id    BIGINT       NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
                                token_hash VARCHAR(255) NOT NULL UNIQUE,
                                expires_at TIMESTAMPTZ  NOT NULL,
                                is_revoked BOOLEAN      NOT NULL DEFAULT FALSE,
                                created_at TIMESTAMPTZ  NOT NULL,
                                updated_at TIMESTAMPTZ  NOT NULL,
                                created_by VARCHAR      NOT NULL,
                                updated_by VARCHAR      NOT NULL
);

-- ============================================================
-- テストデータ
-- ============================================================

-- 1. テナント（users より先に INSERT する必要がある）
INSERT INTO tenants (code, name, is_active, created_at, updated_at, created_by, updated_by)
VALUES ('test-tenant', 'テストテナント', TRUE, NOW(), NOW(), 'system', 'system');

-- 2. ユーザー（password: "password" を BCrypt でハッシュ化した値）
INSERT INTO users (tenant_id, email, password_hash, role, is_active, created_at, updated_at, created_by, updated_by)
VALUES (
           (SELECT id FROM tenants WHERE code = 'test-tenant'),
           'test@example.com',
           '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
           'ROLE_USER',
           TRUE,
           NOW(), NOW(), 'system', 'system'
       );

-- 3. リフレッシュトークン（生トークン: "test-refresh-token" を SHA-256 でハッシュ化した値）
INSERT INTO refresh_tokens (tenant_id, user_id, token_hash, expires_at, is_revoked, created_at, updated_at, created_by, updated_by)
VALUES (
           (SELECT id FROM tenants WHERE code = 'test-tenant'),
           (SELECT id FROM users WHERE email = 'test@example.com'),
           'a0d4b6e8f2c14e6a8b0d2f4a6e8c0b2d4f6a8e0c2b4d6f8a0e2c4b6d8f0a2e4',
           NOW() + INTERVAL '7 days',
           FALSE,
           NOW(), NOW(), 'system', 'system'
       );
