package io.github.aki0057.multitenant.auth.domain.model;

import io.github.aki0057.multitenant.auth.domain.model.vo.RefreshTokenId;
import io.github.aki0057.multitenant.auth.domain.model.vo.TokenHash;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;

import java.time.Instant;

/**
 * リフレッシュトークンを表すドメインモデル。
 * JPA エンティティとは分離した純粋な POJO。
 *
 * <p>スタブ: 本増分では純粋なデータ保持のみ。
 * 期限切れ・失効判定と {@code revoke()} は後続 domain 増分で実装する。</p>
 *
 * @param id        リフレッシュトークンの主キー（未永続化の場合は {@code null}）
 * @param userId    トークンの所有ユーザーの主キー
 * @param tokenHash 生トークンの SHA-256 ハッシュ値
 * @param expiresAt トークンの有効期限
 * @param revoked   失効済みかどうか
 */
// TODO: 期限切れ・失効判定と revoke() を実装（後続 domain 増分）
public record RefreshToken(
        RefreshTokenId id,
        UserId userId,
        TokenHash tokenHash,
        Instant expiresAt,
        boolean revoked
) {}
