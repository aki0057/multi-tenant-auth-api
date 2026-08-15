package io.github.aki0057.multitenant.auth.infrastructure.persistence.repository;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.Email;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantCode;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantId;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;
import io.github.aki0057.multitenant.auth.domain.repository.UserRepository;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository ドメインインターフェースの JPA 実装。
 * UserJpaRepository に委譲し、UserMapper でドメインモデルへ変換して返す。
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<User> findByTenantCodeAndEmail(TenantCode tenantCode, Email email) {
        return userJpaRepository.findByTenant_CodeAndEmail(tenantCode.value(), email.value())
                .map(userMapper::toDomain);
    }

    /**
     * 主キーでユーザーを検索する。
     * UserJpaRepository の主キー検索に委譲し、UserMapper でドメインモデルへ変換して返す。
     *
     * @param userId ユーザーの主キー
     * @return 該当するユーザー（存在しない場合は空）
     */
    @Override
    public Optional<User> findById(UserId userId) {
        return userJpaRepository.findById(userId.value())
                .map(userMapper::toDomain);
    }

    /**
     * 指定されたテナントに所属するユーザーを ID 昇順で検索する。
     * UserJpaRepository#findByTenant_IdOrderByIdAsc に委譲し、UserMapper で 1 件ずつ
     * ドメインモデルへ変換して返す。
     *
     * <p>ID 昇順は SQL の {@code ORDER BY id ASC} で保証されるため、Java 側では並び替えず
     * 取得順をそのまま維持する。</p>
     *
     * <p>{@code is_active} による絞り込みは行わないため、無効ユーザー・無効テナントの
     * ユーザーも除外しない。有効・無効の判定は呼び出し元の責務とする。</p>
     *
     * <p>委譲先の {@code @EntityGraph(attributePaths = "tenant")} により tenant を
     * JOIN FETCH するため、UserMapper が参照する tenant のフィールドで N+1 は発生しない。</p>
     *
     * @param tenantId 検索対象テナントの主キー
     * @return 該当テナントに所属するユーザーを ID 昇順で並べたリスト（存在しない場合は空リスト）
     */
    @Override
    public List<User> findByTenantId(TenantId tenantId) {
        return userJpaRepository.findByTenant_IdOrderByIdAsc(tenantId.value())
                .stream()
                .map(userMapper::toDomain)
                .toList();
    }
}

