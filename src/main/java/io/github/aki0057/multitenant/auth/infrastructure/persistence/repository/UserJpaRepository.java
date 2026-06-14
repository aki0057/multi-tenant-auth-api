package io.github.aki0057.multitenant.auth.infrastructure.persistence.repository;

import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA リポジトリ。
 * テナントコードとメールアドレスで users テーブルを検索する。
 */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    /**
     * tenant.code と email でユーザーを検索する。
     * Spring Data JPA がメソッド名から INNER JOIN クエリを自動生成する。
     */
    Optional<UserJpaEntity> findByTenant_CodeAndEmail(String tenantCode, String email);
}

