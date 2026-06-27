package io.github.aki0057.multitenant.auth.domain.service;

import io.github.aki0057.multitenant.auth.domain.model.User;

/**
 * 認証済みユーザーから JWT アクセストークンを発行するドメインポート。
 * トークン生成方式（jjwt など）の詳細は infrastructure 層の実装に委ねる。
 */
// TODO: 実装クラスは後続の infrastructure(jjwt) 増分で作成する（スタブ）。
public interface AccessTokenProvider {

    /**
     * 認証済みユーザーから JWT アクセストークンを発行する。
     *
     * @param user 認証済みユーザー
     * @return 発行された JWT アクセストークン
     */
    String issue(User user);
}
