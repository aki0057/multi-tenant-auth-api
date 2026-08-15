package io.github.aki0057.multitenant.auth.domain.repository;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.Email;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantCode;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantId;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;

import java.util.List;
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

    /**
     * 主キーでユーザーを検索する。
     *
     * @param userId ユーザーの主キー
     * @return 該当ユーザー（存在しない場合は空）
     */
    Optional<User> findById(UserId userId);

    /**
     * 指定されたテナントに所属するユーザーを検索する。
     *
     * <p>該当テナントに所属するユーザーを<strong>すべて</strong>返す。
     * 無効ユーザー（{@code users.is_active = false}）も除外しない。
     * 有効・無効の判定は呼び出し元の責務とする。</p>
     *
     * <p><strong>並び順の保証は本メソッドの責務とし、ID 昇順で返す。</strong>
     * 呼び出し元は返却された順序をそのまま維持する。</p>
     *
     * @param tenantId 検索対象テナントの主キー
     * @return 該当テナントに所属するユーザーを ID 昇順で並べたリスト。
     *         該当ユーザーが存在しない場合は {@code null} ではなく空リスト
     */
    List<User> findByTenantId(TenantId tenantId);
}

