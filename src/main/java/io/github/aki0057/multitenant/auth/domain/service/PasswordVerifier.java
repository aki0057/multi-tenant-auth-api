package io.github.aki0057.multitenant.auth.domain.service;

import io.github.aki0057.multitenant.auth.domain.model.vo.PasswordHash;
import io.github.aki0057.multitenant.auth.domain.model.vo.RawPassword;

/**
 * パスワード照合を担うドメインポート。
 * 生パスワードがハッシュと一致するかを検証する。
 * BCrypt などの具体的な照合方式は infrastructure 層の実装に委ねる。
 */
public interface PasswordVerifier {

    /**
     * 生パスワードがハッシュ化済みパスワードと一致するかを検証する。
     *
     * @param rawPassword  照合対象の生パスワード
     * @param passwordHash 比較対象のハッシュ化済みパスワード
     * @return 一致する場合 true
     */
    boolean matches(RawPassword rawPassword, PasswordHash passwordHash);
}
