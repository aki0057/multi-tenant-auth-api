package io.github.aki0057.multitenant.auth.domain.service;

import io.github.aki0057.multitenant.auth.domain.model.User;

/**
 * 認証済みユーザーから JWT アクセストークンを発行するドメインポート。
 * トークン生成方式の詳細は infrastructure 層の実装に委ねる。
 */
public interface AccessTokenProvider {

    /**
     * 認証済みユーザーから JWT アクセストークンを発行する。
     *
     * @param user 認証済みユーザー
     * @return 発行された JWT アクセストークン
     */
    String issue(User user);
}
