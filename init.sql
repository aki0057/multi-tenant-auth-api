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
--
-- 全ユーザー共通パスワード: "password"
--   password_hash は "password" を BCrypt（$2a$, cost 10）でハッシュ化した実値。
--   Spring Security の BCryptPasswordEncoder で検証可能。
-- テナントコードは TenantCode VO（^[a-zA-Z0-9]+$）に適合させる（ハイフン不可）。
-- role は Role VO の許容値（USER / ADMIN）のみ。
-- リフレッシュトークンはシードしない（/login から開始する想定）。

-- 1. テナント 2 件（users より先に INSERT する必要がある）
INSERT INTO tenants (code, name, is_active, created_at, updated_at, created_by, updated_by)
VALUES
    ('testTenant', 'テストテナントA', TRUE, NOW(), NOW(), 'system', 'system'),
    ('demoTenant', 'テストテナントB', TRUE, NOW(), NOW(), 'system', 'system');

-- 2. ユーザー（各テナント 3 名 = USER 2 名 + ADMIN 1 名）
--    同一メールを両テナントに配置し、UNIQUE(tenant_id, email) によるテナント分離を体現する。
INSERT INTO users (tenant_id, email, password_hash, role, is_active, created_at, updated_at, created_by, updated_by)
VALUES
    -- testTenant
    ((SELECT id FROM tenants WHERE code = 'testTenant'), 'user1@example.com', '$2a$10$5AA3ksnyeVAshGO4HN5s/.Op0ygx.ahglEi.Di5BI3xSTfjSJWazC', 'USER',  TRUE, NOW(), NOW(), 'system', 'system'),
    ((SELECT id FROM tenants WHERE code = 'testTenant'), 'user2@example.com', '$2a$10$5AA3ksnyeVAshGO4HN5s/.Op0ygx.ahglEi.Di5BI3xSTfjSJWazC', 'USER',  TRUE, NOW(), NOW(), 'system', 'system'),
    ((SELECT id FROM tenants WHERE code = 'testTenant'), 'admin10@example.com', '$2a$10$5AA3ksnyeVAshGO4HN5s/.Op0ygx.ahglEi.Di5BI3xSTfjSJWazC', 'ADMIN', TRUE, NOW(), NOW(), 'system', 'system'),
    -- demoTenant
    ((SELECT id FROM tenants WHERE code = 'demoTenant'), 'user1@example.com', '$2a$10$5AA3ksnyeVAshGO4HN5s/.Op0ygx.ahglEi.Di5BI3xSTfjSJWazC', 'USER',  TRUE, NOW(), NOW(), 'system', 'system'),
    ((SELECT id FROM tenants WHERE code = 'demoTenant'), 'user3@example.com', '$2a$10$5AA3ksnyeVAshGO4HN5s/.Op0ygx.ahglEi.Di5BI3xSTfjSJWazC', 'USER',  TRUE, NOW(), NOW(), 'system', 'system'),
    ((SELECT id FROM tenants WHERE code = 'demoTenant'), 'admin20@example.com', '$2a$10$5AA3ksnyeVAshGO4HN5s/.Op0ygx.ahglEi.Di5BI3xSTfjSJWazC', 'ADMIN', TRUE, NOW(), NOW(), 'system', 'system');
