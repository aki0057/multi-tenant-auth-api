package io.github.aki0057.multitenant.auth.infrastructure.persistence.repository;

import io.github.aki0057.multitenant.auth.config.JpaAuditingConfig;
import io.github.aki0057.multitenant.auth.domain.model.RefreshToken;
import io.github.aki0057.multitenant.auth.domain.model.vo.RefreshTokenId;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantId;
import io.github.aki0057.multitenant.auth.domain.model.vo.TokenHash;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.TenantJpaEntity;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.UserJpaEntity;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.mapper.RefreshTokenMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RefreshTokenRepositoryImpl} の単体テスト。
 *
 * <p>Spring Data JPA リポジトリは {@code @DataJpaTest} で起動した実 H2 DB から
 * {@code @Autowired} で取得し、{@link RefreshTokenMapperImpl} と
 * {@link RefreshTokenRepositoryImpl} はテスト内で直接インスタンス化して手動配線する。</p>
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class RefreshTokenRepositoryImplTest {

    /** SHA-256 の hex 文字列形式（64 桁）を満たすテスト用トークンハッシュ。 */
    private static final String TOKEN_HASH = "a".repeat(64);

    private static final Instant EXPIRES_AT = Instant.parse("2026-07-18T00:00:00Z");

    @Autowired
    private TestEntityManager em;

    @Autowired
    private RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Autowired
    private TenantJpaRepository tenantJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private RefreshTokenRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new RefreshTokenRepositoryImpl(
                refreshTokenJpaRepository,
                new RefreshTokenMapperImpl(),
                tenantJpaRepository,
                userJpaRepository);
    }

    // ---------------------------------------------------------------
    // 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: token_hash が一致するリフレッシュトークンがドメインモデルで返る")
    void findByTokenHash_found() {
        TenantJpaEntity tenant = em.persist(buildTenant("tenant-a"));
        UserJpaEntity user = em.persist(buildUser(tenant, "test@example.com"));
        em.persist(buildRefreshToken(tenant, user, TOKEN_HASH));
        em.flush();

        Optional<RefreshToken> result = repository.findByTokenHash(new TokenHash(TOKEN_HASH));

        assertThat(result).isPresent();
        assertThat(result.get().id().value()).isNotNull();
        assertThat(result.get().tenantId().value()).isEqualTo(tenant.getId());
        assertThat(result.get().userId().value()).isEqualTo(user.getId());
        assertThat(result.get().tokenHash().value()).isEqualTo(TOKEN_HASH);
        assertThat(result.get().expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(result.get().revoked()).isFalse();
    }

    @Test
    @DisplayName("正常系: id が null の新規トークンが INSERT され、採番された id を含めて読み出せる")
    void save_insertsNewToken() {
        TenantJpaEntity tenant = em.persist(buildTenant("tenant-a"));
        UserJpaEntity user = em.persist(buildUser(tenant, "test@example.com"));
        em.flush();

        RefreshToken newToken = new RefreshToken(
                null,
                new TenantId(tenant.getId()),
                new UserId(user.getId()),
                new TokenHash(TOKEN_HASH),
                EXPIRES_AT,
                false);

        RefreshToken saved = repository.save(newToken);
        em.flush();
        em.clear();

        // 返却されたドメインモデルに採番済み id が含まれる
        assertThat(saved.id()).isNotNull();
        assertThat(saved.tenantId().value()).isEqualTo(tenant.getId());
        assertThat(saved.userId().value()).isEqualTo(user.getId());
        assertThat(saved.tokenHash().value()).isEqualTo(TOKEN_HASH);
        assertThat(saved.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(saved.revoked()).isFalse();

        // DB から全フィールドが読み出せる（日時カラムは Auditing が自動設定）
        RefreshTokenJpaEntity found =
                em.find(RefreshTokenJpaEntity.class, saved.id().value());
        assertThat(found.getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(found.getExpiresAt().toInstant()).isEqualTo(EXPIRES_AT);
        assertThat(found.isRevoked()).isFalse();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getCreatedBy()).isEqualTo(String.valueOf(user.getId()));
        assertThat(found.getUpdatedBy()).isEqualTo(String.valueOf(user.getId()));
    }

    @Test
    @DisplayName("正常系: 失効済みトークンの UPDATE で revoked が更新され、created_at / created_by は元の値のまま変わらない")
    void save_updatesRevokedFlagWithoutOverwritingCreatedAt() {
        TenantJpaEntity tenant = em.persist(buildTenant("tenant-a"));
        UserJpaEntity user = em.persist(buildUser(tenant, "test@example.com"));
        RefreshTokenJpaEntity persisted = em.persist(buildRefreshToken(tenant, user, TOKEN_HASH));
        em.flush();
        em.clear();

        // UPDATE 前の DB 保存値を記録する
        RefreshTokenJpaEntity before = em.find(RefreshTokenJpaEntity.class, persisted.getId());
        OffsetDateTime createdAtBefore = before.getCreatedAt();
        String createdByBefore = before.getCreatedBy();
        assertThat(before.isRevoked()).isFalse();
        em.clear();

        // 既存トークンを revoke() して保存する（id あり → UPDATE 経路）
        RefreshToken revoked = new RefreshToken(
                new RefreshTokenId(persisted.getId()),
                new TenantId(tenant.getId()),
                new UserId(user.getId()),
                new TokenHash(TOKEN_HASH),
                EXPIRES_AT,
                false).revoke();

        RefreshToken saved = repository.save(revoked);
        em.flush();
        em.clear();

        assertThat(saved.revoked()).isTrue();

        // revoked のみ更新され、created_at / created_by は元の値のまま
        RefreshTokenJpaEntity after = em.find(RefreshTokenJpaEntity.class, persisted.getId());
        assertThat(after.isRevoked()).isTrue();
        assertThat(after.getCreatedAt()).isEqualTo(createdAtBefore);
        assertThat(after.getCreatedBy()).isEqualTo(createdByBefore);
    }

    // ---------------------------------------------------------------
    // 異常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("異常系: token_hash が一致しない場合は empty が返る")
    void findByTokenHash_notFound() {
        TenantJpaEntity tenant = em.persist(buildTenant("tenant-a"));
        UserJpaEntity user = em.persist(buildUser(tenant, "test@example.com"));
        em.persist(buildRefreshToken(tenant, user, TOKEN_HASH));
        em.flush();

        // 存在しないトークンハッシュで検索
        Optional<RefreshToken> result =
                repository.findByTokenHash(new TokenHash("b".repeat(64)));

        assertThat(result).isEmpty();
    }

    // ---------------------------------------------------------------
    // テストデータビルダー
    // ---------------------------------------------------------------

    private TenantJpaEntity buildTenant(String code) {
        return TenantJpaEntity.builder()
                .code(code)
                .name(code + "-name")
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .createdBy("system")
                .updatedBy("system")
                .build();
    }

    private UserJpaEntity buildUser(TenantJpaEntity tenant, String email) {
        return UserJpaEntity.builder()
                .tenant(tenant)
                .email(email)
                .passwordHash("hashed-password") // パスワード検証は当該テストクラスでは行わない。
                .role("ROLE_USER")
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .createdBy("system")
                .updatedBy("system")
                .build();
    }

    private RefreshTokenJpaEntity buildRefreshToken(
            TenantJpaEntity tenant, UserJpaEntity user, String tokenHash) {
        // createdAt / updatedAt は JPA Auditing が persist 時に自動設定するため指定しない。
        return RefreshTokenJpaEntity.builder()
                .tenant(tenant)
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(EXPIRES_AT.atOffset(ZoneOffset.UTC))
                .revoked(false)
                .createdBy(String.valueOf(user.getId()))
                .updatedBy(String.valueOf(user.getId()))
                .build();
    }
}
