package io.github.aki0057.multitenant.auth.infrastructure.persistence.repository;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantId;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.TenantJpaEntity;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.UserJpaEntity;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.mapper.UserMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link UserRepositoryImpl#findByTenantId(TenantId)} の単体テスト。
 *
 * <p>Spring Data JPA リポジトリは {@code @DataJpaTest} で起動した実 H2 DB から
 * {@code @Autowired} で取得し、{@link UserMapperImpl}（MapStruct 生成クラス）と
 * {@link UserRepositoryImpl} はテスト内で直接インスタンス化して手動配線する。</p>
 *
 * <p>テストデータは {@link TestEntityManager#persist(Object)} で用意し、
 * {@code flush()} / {@code clear()} により永続化コンテキストのキャッシュに依存せず
 * DB から読み直した結果を検証する。</p>
 *
 * <p>引数の {@link TenantId} は {@code null} / 0 以下を VO 生成時に
 * {@link IllegalArgumentException} で弾くため、それらが本リポジトリへ到達することはない。
 * 当該ケースは {@code UserServiceTest} で検証済みであり、本テストクラスの対象外とする。</p>
 */
@DataJpaTest
class UserRepositoryImplTest {

    /** BCrypt 形式を満たすテスト用パスワードハッシュ。 */
    private static final String PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private UserRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new UserRepositoryImpl(userJpaRepository, new UserMapperImpl());
    }

    // ---------------------------------------------------------------
    // 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: 指定テナントに所属するユーザーが全件ドメインモデルで返る")
    void findByTenantId_found() {
        TenantJpaEntity tenant = em.persist(buildTenant("tenantA", true));
        UserJpaEntity admin = em.persist(buildUser(tenant, "admin@example.com", "ADMIN", true));
        UserJpaEntity user = em.persist(buildUser(tenant, "user@example.com", "USER", true));
        em.flush();
        em.clear();

        List<User> result = repository.findByTenantId(new TenantId(tenant.getId()));

        assertThat(result).hasSize(2);

        // 1 件目（ADMIN）の全フィールドが UserMapper の規約どおり変換される
        User first = result.get(0);
        assertThat(first.userId().value()).isEqualTo(admin.getId());
        assertThat(first.tenantId().value()).isEqualTo(tenant.getId());
        assertThat(first.tenantCode().value()).isEqualTo("tenantA");
        assertThat(first.email().value()).isEqualTo("admin@example.com");
        assertThat(first.passwordHash().value()).isEqualTo(PASSWORD_HASH);
        assertThat(first.role().value()).isEqualTo("ADMIN");
        assertThat(first.userIdIsActive()).isTrue();
        assertThat(first.tenantIdIsActive()).isTrue();

        // 2 件目（USER）も同様に変換される
        User second = result.get(1);
        assertThat(second.userId().value()).isEqualTo(user.getId());
        assertThat(second.tenantId().value()).isEqualTo(tenant.getId());
        assertThat(second.tenantCode().value()).isEqualTo("tenantA");
        assertThat(second.email().value()).isEqualTo("user@example.com");
        assertThat(second.role().value()).isEqualTo("USER");
        assertThat(second.userIdIsActive()).isTrue();
        assertThat(second.tenantIdIsActive()).isTrue();
    }

    @Test
    @DisplayName("正常系: 返却順が userId の昇順になる（メールアドレスの辞書順ではない）")
    void findByTenantId_orderedByIdAsc() {
        // 永続化順（= ID 昇順）とメールアドレスの辞書順が一致しないデータを投入する
        TenantJpaEntity tenant = em.persist(buildTenant("tenantA", true));
        em.persist(buildUser(tenant, "zulu@example.com", "USER", true));
        em.persist(buildUser(tenant, "alpha@example.com", "USER", true));
        em.persist(buildUser(tenant, "mike@example.com", "USER", true));
        em.flush();
        em.clear();

        List<User> result = repository.findByTenantId(new TenantId(tenant.getId()));

        assertThat(result).hasSize(3);
        assertThat(result).extracting(u -> u.userId().value()).isSorted();
        // 辞書順ではなく永続化順（ID 昇順）で返る
        assertThat(result).extracting(u -> u.email().value())
                .containsExactly("zulu@example.com", "alpha@example.com", "mike@example.com");
    }

    @Test
    @DisplayName("正常系: 無効ユーザー（is_active = false）も除外されず含まれる")
    void findByTenantId_includesInactiveUser() {
        TenantJpaEntity tenant = em.persist(buildTenant("tenantA", true));
        em.persist(buildUser(tenant, "active@example.com", "USER", true));
        em.persist(buildUser(tenant, "inactive@example.com", "USER", false));
        em.flush();
        em.clear();

        List<User> result = repository.findByTenantId(new TenantId(tenant.getId()));

        // 有効・無効の判定は呼び出し元の責務のため、リポジトリでは絞り込まない
        assertThat(result).hasSize(2);
        assertThat(result).extracting(u -> u.email().value())
                .containsExactly("active@example.com", "inactive@example.com");
        assertThat(result.get(0).userIdIsActive()).isTrue();
        assertThat(result.get(1).userIdIsActive()).isFalse();
    }

    @Test
    @DisplayName("正常系: 無効テナント（is_active = false）でも所属ユーザーが返る")
    void findByTenantId_inactiveTenant() {
        TenantJpaEntity tenant = em.persist(buildTenant("tenantA", false));
        em.persist(buildUser(tenant, "active@example.com", "USER", true));
        em.persist(buildUser(tenant, "inactive@example.com", "USER", false));
        em.flush();
        em.clear();

        List<User> result = repository.findByTenantId(new TenantId(tenant.getId()));

        assertThat(result).hasSize(2);
        // テナント無効は tenantIdIsActive に反映されるだけで、除外はされない
        assertThat(result).allMatch(u -> !u.tenantIdIsActive());
        assertThat(result.get(0).userIdIsActive()).isTrue();
        assertThat(result.get(1).userIdIsActive()).isFalse();
    }

    @Test
    @DisplayName("正常系: 同じメールアドレスが別テナントに存在しても、指定テナントのユーザーのみ返る")
    void findByTenantId_otherTenantExcluded() {
        TenantJpaEntity tenantA = em.persist(buildTenant("tenantA", true));
        TenantJpaEntity tenantB = em.persist(buildTenant("tenantB", true));
        em.persist(buildUser(tenantA, "shared@example.com", "USER", true));
        em.persist(buildUser(tenantB, "shared@example.com", "USER", true));
        em.persist(buildUser(tenantB, "other@example.com", "USER", true));
        em.flush();
        em.clear();

        List<User> result = repository.findByTenantId(new TenantId(tenantA.getId()));

        // 他テナントのユーザーは 1 件も含まれない
        assertThat(result).hasSize(1);
        assertThat(result.get(0).tenantId().value()).isEqualTo(tenantA.getId());
        assertThat(result.get(0).tenantCode().value()).isEqualTo("tenantA");
        assertThat(result).noneMatch(u -> u.tenantId().value().equals(tenantB.getId()));
    }

    // ---------------------------------------------------------------
    // 異常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("異常系: ユーザーが 1 人も所属しないテナントの場合は空リストが返る")
    void findByTenantId_noUsers() {
        TenantJpaEntity emptyTenant = em.persist(buildTenant("tenantA", true));
        TenantJpaEntity otherTenant = em.persist(buildTenant("tenantB", true));
        em.persist(buildUser(otherTenant, "user@example.com", "USER", true));
        em.flush();
        em.clear();

        List<User> result = repository.findByTenantId(new TenantId(emptyTenant.getId()));

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("異常系: 存在しないテナント ID の場合は例外をスローせず空リストが返る")
    void findByTenantId_unknownTenantId() {
        TenantJpaEntity tenant = em.persist(buildTenant("tenantA", true));
        em.persist(buildUser(tenant, "user@example.com", "USER", true));
        em.flush();
        em.clear();

        // 存在しないテナント ID で検索
        List<User> result = repository.findByTenantId(new TenantId(Long.MAX_VALUE));

        assertThat(result).isNotNull().isEmpty();
    }

    // ---------------------------------------------------------------
    // テストデータビルダー
    // ---------------------------------------------------------------

    /**
     * テナントの JPA エンティティを組み立てる。
     *
     * @param code   テナントコード（TenantCode は半角英数字のみ許容する）
     * @param active 有効フラグ（{@code tenants.is_active}）
     * @return 未永続化の TenantJpaEntity
     */
    private TenantJpaEntity buildTenant(String code, boolean active) {
        return TenantJpaEntity.builder()
                .code(code)
                .name(code + "-name")
                .active(active)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .createdBy("system")
                .updatedBy("system")
                .build();
    }

    /**
     * ユーザーの JPA エンティティを組み立てる。
     *
     * @param tenant 所属テナント
     * @param email  メールアドレス
     * @param role   ロール（Role は {@code "ADMIN"} / {@code "USER"} のみ許容する）
     * @param active 有効フラグ（{@code users.is_active}）
     * @return 未永続化の UserJpaEntity
     */
    private UserJpaEntity buildUser(
            TenantJpaEntity tenant, String email, String role, boolean active) {
        return UserJpaEntity.builder()
                .tenant(tenant)
                .email(email)
                .passwordHash(PASSWORD_HASH) // パスワード検証は当該テストクラスでは行わない。
                .role(role)
                .active(active)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .createdBy("system")
                .updatedBy("system")
                .build();
    }
}
