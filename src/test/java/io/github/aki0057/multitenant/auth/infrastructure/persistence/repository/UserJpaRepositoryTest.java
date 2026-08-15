package io.github.aki0057.multitenant.auth.infrastructure.persistence.repository;

import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.TenantJpaEntity;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.UserJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserJpaRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserJpaRepository userJpaRepository;

    // ---------------------------------------------------------------
    // 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: tenant.code と email が一致するユーザーが返る")
    void findByTenantCodeAndEmail_found() {
        TenantJpaEntity tenant = em.persist(buildTenant("tenant-a"));
        em.persist(buildUser(tenant, "test@example.com"));
        em.flush();

        Optional<UserJpaEntity> result =
                userJpaRepository.findByTenant_CodeAndEmail("tenant-a", "test@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        assertThat(result.get().getTenant().getCode()).isEqualTo("tenant-a");
    }

    @Test
    @DisplayName("正常系: tenant.code が一致しない場合は empty が返る")
    void findByTenantCodeAndEmail_tenantNotMatch() {
        TenantJpaEntity tenant = em.persist(buildTenant("tenant-a"));
        em.persist(buildUser(tenant, "test@example.com"));
        em.flush();

        // 存在しない tenant-b で検索
        Optional<UserJpaEntity> result =
                userJpaRepository.findByTenant_CodeAndEmail("tenant-b", "test@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("正常系: email が一致しない場合は empty が返る")
    void findByTenantCodeAndEmail_emailNotMatch() {
        TenantJpaEntity tenant = em.persist(buildTenant("tenant-a"));
        em.persist(buildUser(tenant, "test@example.com"));
        em.flush();

        // 存在しない other@example.com で検索
        Optional<UserJpaEntity> result =
                userJpaRepository.findByTenant_CodeAndEmail("tenant-a", "other@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("正常系: 主キーが一致するユーザーが返る")
    void findById_found() {
        TenantJpaEntity tenant = em.persist(buildTenant("tenant-a"));
        UserJpaEntity user = em.persist(buildUser(tenant, "test@example.com"));
        em.flush();

        Optional<UserJpaEntity> result = userJpaRepository.findById(user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(user.getId());
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("正常系: tenant.id が一致するユーザーが ID 昇順で返り、他テナントのユーザーは含まれない")
    void findByTenant_IdOrderByIdAsc_found() {
        // 永続化順（= ID 昇順）とメールアドレスの辞書順が一致しないデータを投入する
        TenantJpaEntity tenantA = em.persist(buildTenant("tenant-a"));
        TenantJpaEntity tenantB = em.persist(buildTenant("tenant-b"));
        em.persist(buildUser(tenantA, "zulu@example.com"));
        em.persist(buildUser(tenantA, "alpha@example.com"));
        em.persist(buildUser(tenantB, "other@example.com"));
        em.flush();
        em.clear();

        List<UserJpaEntity> result =
                userJpaRepository.findByTenant_IdOrderByIdAsc(tenantA.getId());

        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserJpaEntity::getId).isSorted();
        assertThat(result).extracting(UserJpaEntity::getEmail)
                .containsExactly("zulu@example.com", "alpha@example.com");
        // 他テナントのユーザーは含まれない
        assertThat(result).allMatch(user -> user.getTenant().getId().equals(tenantA.getId()));
    }

    @Test
    @DisplayName("正常系: is_active = false のユーザーも除外されずに返る")
    void findByTenant_IdOrderByIdAsc_includesInactiveUser() {
        TenantJpaEntity tenant = em.persist(buildTenant("tenant-a"));
        em.persist(buildUser(tenant, "active@example.com", true));
        em.persist(buildUser(tenant, "inactive@example.com", false));
        em.flush();
        em.clear();

        List<UserJpaEntity> result =
                userJpaRepository.findByTenant_IdOrderByIdAsc(tenant.getId());

        // クエリに is_active の絞り込みは入っていない
        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserJpaEntity::getEmail)
                .containsExactly("active@example.com", "inactive@example.com");
        assertThat(result.get(1).isActive()).isFalse();
    }

    // ---------------------------------------------------------------
    // 異常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("異常系: 該当ユーザーが存在しないテナント ID の場合は空リストが返る")
    void findByTenant_IdOrderByIdAsc_notFound() {
        TenantJpaEntity tenant = em.persist(buildTenant("tenant-a"));
        em.persist(buildUser(tenant, "test@example.com"));
        em.flush();
        em.clear();

        // 存在しないテナント ID で検索
        List<UserJpaEntity> result =
                userJpaRepository.findByTenant_IdOrderByIdAsc(Long.MAX_VALUE);

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("異常系: 存在しない主キーの場合は empty が返る")
    void findById_notFound() {
        TenantJpaEntity tenant = em.persist(buildTenant("tenant-a"));
        em.persist(buildUser(tenant, "test@example.com"));
        em.flush();

        // 存在しない主キーで検索
        Optional<UserJpaEntity> result = userJpaRepository.findById(Long.MAX_VALUE);

        assertThat(result).isEmpty();
    }

    // ---------------------------------------------------------------
    // 境界値
    // ---------------------------------------------------------------

    @Test
    @DisplayName("境界値: 同じメールアドレスが別テナントに存在する場合、指定テナントのユーザーのみ返る")
    void findByTenantCodeAndEmail_sameEmailInDifferentTenants() {
        // 同一メールアドレスを 2 つのテナントにそれぞれ登録
        TenantJpaEntity tenantA = em.persist(buildTenant("tenant-a"));
        TenantJpaEntity tenantB = em.persist(buildTenant("tenant-b"));
        em.persist(buildUser(tenantA, "shared@example.com"));
        em.persist(buildUser(tenantB, "shared@example.com"));
        em.flush();

        Optional<UserJpaEntity> resultA =
                userJpaRepository.findByTenant_CodeAndEmail("tenant-a", "shared@example.com");
        Optional<UserJpaEntity> resultB =
                userJpaRepository.findByTenant_CodeAndEmail("tenant-b", "shared@example.com");

        // 各テナントで正しいユーザーが返る
        assertThat(resultA).isPresent();
        assertThat(resultA.get().getTenant().getCode()).isEqualTo("tenant-a");

        assertThat(resultB).isPresent();
        assertThat(resultB.get().getTenant().getCode()).isEqualTo("tenant-b");
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

    /**
     * 有効フラグを指定してユーザーの JPA エンティティを組み立てる。
     *
     * @param tenant 所属テナント
     * @param email  メールアドレス
     * @param active 有効フラグ（{@code users.is_active}）
     * @return 未永続化の UserJpaEntity
     */
    private UserJpaEntity buildUser(TenantJpaEntity tenant, String email, boolean active) {
        return UserJpaEntity.builder()
                .tenant(tenant)
                .email(email)
                .passwordHash("hashed-password") // パスワード検証は当該テストクラスでは行わない。
                .role("ROLE_USER")
                .active(active)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .createdBy("system")
                .updatedBy("system")
                .build();
    }
}

