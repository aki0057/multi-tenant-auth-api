package io.github.aki0057.multitenant.auth.infrastructure.persistence.repository;

import io.github.aki0057.multitenant.auth.domain.model.RefreshToken;
import io.github.aki0057.multitenant.auth.domain.model.vo.TokenHash;
import io.github.aki0057.multitenant.auth.domain.repository.RefreshTokenRepository;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.TenantJpaEntity;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.UserJpaEntity;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * RefreshTokenRepository ドメインインターフェースの JPA 実装。
 * RefreshTokenJpaRepository に委譲し、RefreshTokenMapper でドメインモデルへ変換して返す。
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;
    private final RefreshTokenMapper refreshTokenMapper;
    private final TenantJpaRepository tenantJpaRepository;
    private final UserJpaRepository userJpaRepository;

    /**
     * トークンのハッシュ値で該当するリフレッシュトークンを検索する。
     *
     * @param tokenHash 生トークンの SHA-256 ハッシュ値
     * @return 該当するリフレッシュトークン（存在しない場合は空）
     */
    @Override
    public Optional<RefreshToken> findByTokenHash(TokenHash tokenHash) {
        return refreshTokenJpaRepository.findByTokenHash(tokenHash.value())
                .map(refreshTokenMapper::toDomain);
    }

    /**
     * リフレッシュトークンを保存する。
     *
     * <p>INSERT / UPDATE の両経路を分岐なしで扱う。Spring Data JPA は
     * id が {@code null} なら persist（INSERT）、非 {@code null} なら merge（UPDATE）を
     * 自動選択する。UPDATE 経路では、エンティティの {@code created_at} / {@code created_by} が
     * {@code updatable = false} のため SET 句に含まれず、DB の既存値は上書きされない。
     * {@code updated_at} は JPA Auditing（{@code @LastModifiedDate}）が自動更新する。</p>
     *
     * <p>{@code @ManyToOne} の tenant / user 参照は {@code getReferenceById}
     * （追加の SELECT を発行しないプロキシ取得）で解決する。</p>
     *
     * @param refreshToken 保存するリフレッシュトークン
     * @return 保存されたリフレッシュトークン（INSERT の場合は採番された id を含む）
     */
    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        TenantJpaEntity tenant =
                tenantJpaRepository.getReferenceById(refreshToken.tenantId().value());
        UserJpaEntity user =
                userJpaRepository.getReferenceById(refreshToken.userId().value());
        RefreshTokenJpaEntity entity = refreshTokenMapper.toEntity(refreshToken, tenant, user);
        return refreshTokenMapper.toDomain(refreshTokenJpaRepository.save(entity));
    }
}
