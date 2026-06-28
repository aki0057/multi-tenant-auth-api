package io.github.aki0057.multitenant.auth.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.TenantJpaEntity;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.UserJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link UserMapper#toDomain(UserJpaEntity)} の単体テスト。
 *
 * <p>MapStruct が生成する実装 {@link UserMapperImpl} を直接インスタンス化して検証する。</p>
 */
class UserMapperTest {

    private final UserMapper userMapper = new UserMapperImpl();

    @Test
    @DisplayName("正常系: 全フィールドが User ドメインモデルへ正しくマッピングされる。")
    void toDomain_allFieldsMapped() {
        TenantJpaEntity tenant = TenantJpaEntity.builder()
                .id(10L)
                .code("acme")
                .active(true)
                .build();
        UserJpaEntity entity = UserJpaEntity.builder()
                .id(1L)
                .tenant(tenant)
                .email("user@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .role("ADMIN")
                .active(true)
                .build();

        User user = userMapper.toDomain(entity);

        assertThat(user.userId().value()).isEqualTo(1L);
        assertThat(user.tenantId().value()).isEqualTo(10L);
        assertThat(user.tenantCode().value()).isEqualTo("acme");
        assertThat(user.email().value()).isEqualTo("user@example.com");
        assertThat(user.passwordHash().value()).isEqualTo("$2a$10$hashedpassword");
        assertThat(user.role().value()).isEqualTo("ADMIN");
        assertThat(user.userIdIsActive()).isTrue();
        assertThat(user.tenantIdIsActive()).isTrue();
    }

    @Test
    @DisplayName("正常系: 無効フラグ（active=false）がそれぞれ userIdIsActive / tenantIdIsActive へマッピングされる。")
    void toDomain_inactiveFlagsMapped() {
        TenantJpaEntity tenant = TenantJpaEntity.builder()
                .id(10L)
                .code("acme")
                .active(false)
                .build();
        UserJpaEntity entity = UserJpaEntity.builder()
                .id(1L)
                .tenant(tenant)
                .email("user@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .role("ADMIN")
                .active(false)
                .build();

        User user = userMapper.toDomain(entity);

        assertThat(user.userIdIsActive()).isFalse();
        assertThat(user.tenantIdIsActive()).isFalse();
    }

    @Test
    @DisplayName("異常系: tenant.id が null の場合 TenantId 生成で IllegalArgumentException がスローされる。")
    void toDomain_tenantIdNull() {
        TenantJpaEntity tenant = TenantJpaEntity.builder()
                .id(null)
                .code("acme")
                .active(true)
                .build();
        UserJpaEntity entity = UserJpaEntity.builder()
                .id(1L)
                .tenant(tenant)
                .email("user@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .role("ADMIN")
                .active(true)
                .build();

        assertThatThrownBy(() -> userMapper.toDomain(entity))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: tenant.id が 0 以下の場合 TenantId 生成で IllegalArgumentException がスローされる。")
    void toDomain_tenantIdNotPositive() {
        TenantJpaEntity tenant = TenantJpaEntity.builder()
                .id(0L)
                .code("acme")
                .active(true)
                .build();
        UserJpaEntity entity = UserJpaEntity.builder()
                .id(1L)
                .tenant(tenant)
                .email("user@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .role("ADMIN")
                .active(true)
                .build();

        assertThatThrownBy(() -> userMapper.toDomain(entity))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
