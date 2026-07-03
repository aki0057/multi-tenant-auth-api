package io.github.aki0057.multitenant.auth.domain.service;

import io.github.aki0057.multitenant.auth.domain.model.vo.RawRefreshToken;
import io.github.aki0057.multitenant.auth.domain.model.vo.TokenHash;

/**
 * 生のリフレッシュトークンをハッシュ化するドメインポート。
 * SHA-256 によるハッシュ計算の詳細は infrastructure 層の実装に委ねる。
 */
// TODO: 実装（後続 infrastructure 増分）
public interface RefreshTokenHasher {

    /**
     * 生のリフレッシュトークンを SHA-256 でハッシュ化する。
     *
     * @param rawRefreshToken 生のリフレッシュトークン
     * @return ハッシュ値（hex 文字列）
     */
    TokenHash hash(RawRefreshToken rawRefreshToken);
}
