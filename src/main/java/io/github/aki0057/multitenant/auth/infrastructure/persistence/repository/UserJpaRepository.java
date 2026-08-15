package io.github.aki0057.multitenant.auth.infrastructure.persistence.repository;

import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA リポジトリ。
 * テナントコードとメールアドレスによる検索、およびテナント ID による所属ユーザーの一覧検索で
 * users テーブルを検索する。
 */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    /**
     * tenant.code と email でユーザーを検索する。
     * Spring Data JPA がメソッド名から INNER JOIN クエリを自動生成する。
     */
    Optional<UserJpaEntity> findByTenant_CodeAndEmail(String tenantCode, String email);

    /**
     * tenant.id が一致するユーザーを ID 昇順で検索する。
     *
     * <p>Spring Data JPA がメソッド名から {@code WHERE tenant_id = ? ORDER BY id ASC} を
     * 自動生成する。{@code Tenant_Id} は {@code @ManyToOne} である tenant の主キーを指す。
     * 並び順は SQL の {@code ORDER BY} で保証するため、呼び出し元で並び替える必要はない。</p>
     *
     * <p>{@code is_active} による絞り込みは行わないため、無効ユーザー・無効テナントの
     * ユーザーも返る。</p>
     *
     * @param tenantId 検索対象テナントの主キー
     * @return 該当テナントに所属するユーザーを ID 昇順で並べたリスト（該当なしの場合は空リスト）
     */
    @EntityGraph(attributePaths = "tenant")
    List<UserJpaEntity> findByTenant_IdOrderByIdAsc(Long tenantId);
}

