package io.github.aki0057.multitenant.auth.domain.model;

import io.github.aki0057.multitenant.auth.domain.model.vo.RefreshTokenId;
import io.github.aki0057.multitenant.auth.domain.model.vo.TokenHash;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;

import java.time.Instant;

/**
 * リフレッシュトークンを表すドメインモデル。
 * JPA エンティティとは分離した純粋な POJO。
 *
 * <p>期限切れ判定（{@link #isExpired(Instant)}）・失効判定（{@link #isRevoked()}）・
 * 失効操作（{@link #revoke()}）を提供する。record のため immutable であり、
 * {@link #revoke()} は自身を変更せず失効済みの新しいインスタンスを返す。</p>
 *
 * @param id        リフレッシュトークンの主キー（未永続化の場合は {@code null}）
 * @param userId    トークンの所有ユーザーの主キー
 * @param tokenHash 生トークンの SHA-256 ハッシュ値
 * @param expiresAt トークンの有効期限
 * @param revoked   失効済みかどうか
 */
public record RefreshToken(
        RefreshTokenId id,
        UserId userId,
        TokenHash tokenHash,
        Instant expiresAt,
        boolean revoked
) {

    /**
     * トークンが期限切れかどうかを判定する。
     *
     * @param now 判定基準となる現在時刻
     * @return 有効期限（{@code expiresAt}）が {@code now} より過去であれば {@code true}
     */
    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    /**
     * トークンが失効済みかどうかを判定する。
     *
     * @return 失効済み（{@code revoked == true}）であれば {@code true}
     */
    public boolean isRevoked() {
        return revoked;
    }

    /**
     * トークンを失効させる。
     *
     * <p>record のため自身の状態は変更せず、{@code revoked = true} とした
     * 新しいインスタンスを返す。他のフィールド（{@code id} / {@code userId} /
     * {@code tokenHash} / {@code expiresAt}）は元の値をそのまま引き継ぐ。</p>
     *
     * @return 失効済み（{@code revoked = true}）の新しい {@code RefreshToken} インスタンス
     */
    public RefreshToken revoke() {
        return new RefreshToken(id, userId, tokenHash, expiresAt, true);
    }
}
