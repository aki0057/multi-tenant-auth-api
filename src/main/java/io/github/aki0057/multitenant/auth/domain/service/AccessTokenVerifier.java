package io.github.aki0057.multitenant.auth.domain.service;

import io.github.aki0057.multitenant.auth.domain.model.AuthenticatedUser;

/**
 * JWT アクセストークンを検証し、認証情報を復元するドメインポート。
 *
 * <p>{@link AccessTokenProvider} がトークンの発行を担うのに対し、本ポートは
 * トークンの検証（署名・有効期限など）とクレームからの {@link AuthenticatedUser} 復元を担う。
 * HS256 パースなどの具体的な検証処理は infrastructure 層の実装に委ねる。</p>
 */
public interface AccessTokenVerifier {

    /**
     * JWT アクセストークンを検証し、認証情報を復元する。
     *
     * @param token 検証対象の JWT アクセストークン
     * @return トークンのクレームから復元した認証情報
     */
    AuthenticatedUser verify(String token);
    // TODO: 増分2（infrastructure.security の JwtAccessTokenVerifier）で本実装する
}
