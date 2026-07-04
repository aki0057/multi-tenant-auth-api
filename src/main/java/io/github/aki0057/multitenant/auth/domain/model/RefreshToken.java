package io.github.aki0057.multitenant.auth.domain.model;

import io.github.aki0057.multitenant.auth.domain.model.vo.RefreshTokenId;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantId;
import io.github.aki0057.multitenant.auth.domain.model.vo.TokenHash;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;

import java.time.Instant;

/**
 * リフレッシュトークンを表すドメインモデル。
 * JPA エンティティとは分離した純粋な POJO。
 *
 * <p>使用可能判定（{@link #isValid(Instant)}）・
 * 失効操作（{@link #revoke()}）を提供する。record のため immutable であり、
 * {@link #revoke()} は自身を変更せず失効済みの新しいインスタンスを返す。</p>
 *
 * @param id        リフレッシュトークンの主キー（未永続化の場合は {@code null}）
 * @param tenantId  トークンの所有ユーザーが属するテナントの主キー
 * @param userId    トークンの所有ユーザーの主キー
 * @param tokenHash 生トークンの SHA-256 ハッシュ値
 * @param expiresAt トークンの有効期限
 * @param revoked   失効済みかどうか
 */
public record RefreshToken(
        RefreshTokenId id,
        TenantId tenantId,
        UserId userId,
        TokenHash tokenHash,
        Instant expiresAt,
        boolean revoked
) {

    /**
     * トークンが使用可能かどうかを判定する。
     *
     * <p>「リフレッシュトークンが使用可能である」とは、失効済みでなく
     * （{@code revoked} が {@code false}）、かつ期限切れでない
     * （{@code expiresAt}）が {@code now} より過去でない状態を指す。
     * 例外はスローしない。</p>
     *
     * @param now 判定基準となる現在時刻
     * @return 失効済みでなく、かつ期限切れでない場合は {@code true}
     */
    public boolean isValid(Instant now) {
        return !revoked && !expiresAt.isBefore(now);
    }

    /**
     * トークンを失効させる。
     *
     * <p>record のため自身の状態は変更せず、{@code revoked = true} とした
     * 新しいインスタンスを返す。他のフィールド（{@code id} / {@code tenantId} /
     * {@code userId} / {@code tokenHash} / {@code expiresAt}）は元の値をそのまま引き継ぐ。</p>
     *
     * @return 失効済み（{@code revoked = true}）の新しい {@code RefreshToken} インスタンス
     */
    public RefreshToken revoke() {
        return new RefreshToken(id, tenantId, userId, tokenHash, expiresAt, true);
    }
}
