package io.github.aki0057.multitenant.auth.domain.repository;

import io.github.aki0057.multitenant.auth.domain.model.RefreshToken;
import io.github.aki0057.multitenant.auth.domain.model.vo.TokenHash;

import java.util.Optional;

/**
 * RefreshToken リポジトリのドメインインターフェース。
 * インフラ実装の詳細（JPAなど）に依存しない。
 */
// TODO: 実装（後続 infrastructure 増分）
public interface RefreshTokenRepository {

    /**
     * トークンのハッシュ値で該当するリフレッシュトークンを検索する。
     *
     * @param tokenHash 生トークンの SHA-256 ハッシュ値
     * @return 該当するリフレッシュトークン（存在しない場合は空）
     */
    Optional<RefreshToken> findByTokenHash(TokenHash tokenHash);

    /**
     * リフレッシュトークンを保存する。
     * 新規トークンの永続化と、既存トークンの失効状態更新の両方に使用する。
     *
     * @param refreshToken 保存するリフレッシュトークン
     * @return 保存されたリフレッシュトークン
     */
    RefreshToken save(RefreshToken refreshToken);
}
