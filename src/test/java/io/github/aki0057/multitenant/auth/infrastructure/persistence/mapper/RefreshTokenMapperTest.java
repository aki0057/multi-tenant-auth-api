package io.github.aki0057.multitenant.auth.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aki0057.multitenant.auth.domain.model.RefreshToken;
import io.github.aki0057.multitenant.auth.domain.model.vo.RefreshTokenId;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantId;
import io.github.aki0057.multitenant.auth.domain.model.vo.TokenHash;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.TenantJpaEntity;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.UserJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * {@link RefreshTokenMapper} の単体テスト。
 *
 * <p>MapStruct が生成する実装 {@link RefreshTokenMapperImpl} を直接インスタンス化して検証する。</p>
 */
class RefreshTokenMapperTest {

    /** SHA-256 の hex 文字列形式（64 桁）を満たすテスト用トークンハッシュ。 */
    private static final String TOKEN_HASH = "a".repeat(64);

    private static final Instant EXPIRES_AT = Instant.parse("2026-07-18T00:00:00Z");

    private final RefreshTokenMapper refreshTokenMapper = new RefreshTokenMapperImpl();

    // ---------------------------------------------------------------
    // 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: 全フィールドが RefreshToken ドメインモデルへ正しくマッピングされる。")
    void toDomain_allFieldsMapped() {
        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.builder()
                .id(100L)
                .tenant(buildTenant(10L))
                .user(buildUser(1L))
                .tokenHash(TOKEN_HASH)
                .expiresAt(EXPIRES_AT.atOffset(ZoneOffset.UTC))
                .revoked(true)
                .build();

        RefreshToken domain = refreshTokenMapper.toDomain(entity);

        assertThat(domain.id().value()).isEqualTo(100L);
        assertThat(domain.tenantId().value()).isEqualTo(10L);
        assertThat(domain.userId().value()).isEqualTo(1L);
        assertThat(domain.tokenHash().value()).isEqualTo(TOKEN_HASH);
        assertThat(domain.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(domain.revoked()).isTrue();
    }

    @Test
    @DisplayName("正常系: 全フィールドが JPA エンティティへ正しくマッピングされる（createdBy / updatedBy は userId の文字列化と一致する）。")
    void toEntity_allFieldsMapped() {
        RefreshToken domain = new RefreshToken(
                new RefreshTokenId(100L),
                new TenantId(10L),
                new UserId(1L),
                new TokenHash(TOKEN_HASH),
                EXPIRES_AT,
                true);
        TenantJpaEntity tenant = buildTenant(10L);
        UserJpaEntity user = buildUser(1L);

        RefreshTokenJpaEntity entity = refreshTokenMapper.toEntity(domain, tenant, user);

        assertThat(entity.getId()).isEqualTo(100L);
        assertThat(entity.getTenant()).isSameAs(tenant);
        assertThat(entity.getUser()).isSameAs(user);
        assertThat(entity.getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(entity.getExpiresAt().toInstant()).isEqualTo(EXPIRES_AT);
        assertThat(entity.isRevoked()).isTrue();
        assertThat(entity.getCreatedBy()).isEqualTo("1");
        assertThat(entity.getUpdatedBy()).isEqualTo("1");
        // 日時カラムは JPA Auditing の管理対象のためマッピングされない
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("正常系: 未永続化（id が null）のドメインモデルはエンティティの id も null になる。")
    void toEntity_nullIdMappedToNull() {
        RefreshToken domain = new RefreshToken(
                null,
                new TenantId(10L),
                new UserId(1L),
                new TokenHash(TOKEN_HASH),
                EXPIRES_AT,
                false);

        RefreshTokenJpaEntity entity =
                refreshTokenMapper.toEntity(domain, buildTenant(10L), buildUser(1L));

        assertThat(entity.getId()).isNull();
        assertThat(entity.isRevoked()).isFalse();
    }

    // ---------------------------------------------------------------
    // 異常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("異常系: tenant.id が null の場合 TenantId 生成で IllegalArgumentException がスローされる。")
    void toDomain_tenantIdNull() {
        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.builder()
                .id(100L)
                .tenant(buildTenant(null))
                .user(buildUser(1L))
                .tokenHash(TOKEN_HASH)
                .expiresAt(EXPIRES_AT.atOffset(ZoneOffset.UTC))
                .revoked(false)
                .build();

        assertThatThrownBy(() -> refreshTokenMapper.toDomain(entity))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: user.id が 0 以下の場合 UserId 生成で IllegalArgumentException がスローされる。")
    void toDomain_userIdNotPositive() {
        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.builder()
                .id(100L)
                .tenant(buildTenant(10L))
                .user(buildUser(0L))
                .tokenHash(TOKEN_HASH)
                .expiresAt(EXPIRES_AT.atOffset(ZoneOffset.UTC))
                .revoked(false)
                .build();

        assertThatThrownBy(() -> refreshTokenMapper.toDomain(entity))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: tokenHash が SHA-256 の hex 形式でない場合 TokenHash 生成で IllegalArgumentException がスローされる。")
    void toDomain_tokenHashInvalid() {
        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.builder()
                .id(100L)
                .tenant(buildTenant(10L))
                .user(buildUser(1L))
                .tokenHash("not-a-sha256-hex")
                .expiresAt(EXPIRES_AT.atOffset(ZoneOffset.UTC))
                .revoked(false)
                .build();

        assertThatThrownBy(() -> refreshTokenMapper.toDomain(entity))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------
    // テストデータビルダー
    // ---------------------------------------------------------------

    private TenantJpaEntity buildTenant(Long id) {
        return TenantJpaEntity.builder()
                .id(id)
                .code("tenant-a")
                .name("tenant-a-name")
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .createdBy("system")
                .updatedBy("system")
                .build();
    }

    private UserJpaEntity buildUser(Long id) {
        return UserJpaEntity.builder()
                .id(id)
                .email("test@example.com")
                .passwordHash("hashed-password") // パスワード検証は当該テストクラスでは行わない。
                .role("ROLE_USER")
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .createdBy("system")
                .updatedBy("system")
                .build();
    }
}
