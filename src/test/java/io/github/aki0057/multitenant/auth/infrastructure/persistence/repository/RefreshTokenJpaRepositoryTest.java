package io.github.aki0057.multitenant.auth.infrastructure.persistence.repository;

import io.github.aki0057.multitenant.auth.config.JpaAuditingConfig;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.TenantJpaEntity;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.UserJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class RefreshTokenJpaRepositoryTest {

    /** SHA-256 の hex 文字列形式（64 桁）を満たすテスト用トークンハッシュ。 */
    private static final String TOKEN_HASH = "a".repeat(64);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private RefreshTokenJpaRepository refreshTokenJpaRepository;

    // ---------------------------------------------------------------
    // 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: token_hash が一致するリフレッシュトークンが返る")
    void findByTokenHash_found() {
        TenantJpaEntity tenant = em.persist(buildTenant("tenant-a"));
        UserJpaEntity user = em.persist(buildUser(tenant, "test@example.com"));
        em.persist(buildRefreshToken(tenant, user, TOKEN_HASH));
        em.flush();

        Optional<RefreshTokenJpaEntity> result =
                refreshTokenJpaRepository.findByTokenHash(TOKEN_HASH);

        assertThat(result).isPresent();
        assertThat(result.get().getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(result.get().getTenant().getCode()).isEqualTo("tenant-a");
        assertThat(result.get().getUser().getEmail()).isEqualTo("test@example.com");
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
        Optional<RefreshTokenJpaEntity> result =
                refreshTokenJpaRepository.findByTokenHash("b".repeat(64));

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
                .expiresAt(OffsetDateTime.of(2026, 7, 18, 0, 0, 0, 0, ZoneOffset.UTC))
                .revoked(false)
                .createdBy("1")
                .updatedBy("1")
                .build();
    }
}
