package io.github.aki0057.multitenant.auth.domain.service;

import io.github.aki0057.multitenant.auth.domain.model.vo.RawRefreshToken;

/**
 * 生のリフレッシュトークンを生成するドメインポート。
 * セキュアランダムなトークン生成方式の詳細は infrastructure 層の実装に委ねる。
 */
public interface RefreshTokenGenerator {

    /**
     * セキュアランダムな生リフレッシュトークンを生成する。
     *
     * @return 生成された生リフレッシュトークン
     */
    RawRefreshToken generate();
}
