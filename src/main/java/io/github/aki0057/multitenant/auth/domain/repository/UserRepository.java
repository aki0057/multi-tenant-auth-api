package io.github.aki0057.multitenant.auth.domain.repository;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.Email;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantCode;

import java.util.Optional;

/**
 * User リポジトリのドメインインターフェース。
 * インフラ実装の詳細（JPAなど）に依存しない。
 */
public interface UserRepository {

    /**
     * テナントコードとメールアドレスでユーザーを検索する。
     *
     * @param tenantCode テナントのコード
     * @param email      ユーザーのメールアドレス
     * @return 該当ユーザー（存在しない場合は空）
     */
    Optional<User> findByTenantCodeAndEmail(TenantCode tenantCode, Email email);
}

