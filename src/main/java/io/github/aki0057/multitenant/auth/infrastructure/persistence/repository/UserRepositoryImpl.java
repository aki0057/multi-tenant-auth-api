package io.github.aki0057.multitenant.auth.infrastructure.persistence.repository;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.Email;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantCode;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;
import io.github.aki0057.multitenant.auth.domain.repository.UserRepository;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
     * 主キーでユーザーを検索するスタブ実装。
     *
     * @param userId ユーザーの主キー
     * @return なし（常に {@link UnsupportedOperationException} をスローする）
     */
    // TODO: UserRepositoryImpl#findById を実装（後続 infrastructure 増分）
    @Override
    public Optional<User> findById(UserId userId) {
        throw new UnsupportedOperationException("UserRepositoryImpl#findById は未実装です");
    }
}

