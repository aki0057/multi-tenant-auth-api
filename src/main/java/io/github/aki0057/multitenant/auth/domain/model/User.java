package io.github.aki0057.multitenant.auth.domain.model;

import io.github.aki0057.multitenant.auth.domain.exception.AuthenticationFailedException;
import io.github.aki0057.multitenant.auth.domain.model.vo.*;
import io.github.aki0057.multitenant.auth.domain.service.PasswordVerifier;

/**
 * User ドメインモデル。
 * JPAエンティティとは分離し、ビジネスロジックのみを持つ純粋なPOJO。
 */
public record User(
        UserId userId,
        TenantId tenantId,
        TenantCode tenantCode,
        Email email,
        PasswordHash passwordHash,
        Role role,
        boolean userIdIsActive,
        boolean tenantIdIsActive
) {

    /**
     * このユーザーが有効かを判定する。
     * 「有効なユーザー」とは、ユーザー自身が有効（{@code userIdIsActive}）かつ
     * 所属テナントも有効（{@code tenantIdIsActive}）であることを指す。
     *
     * @return ユーザー自身と所属テナントの両方が有効な場合は {@code true}、それ以外は {@code false}
     */
    public boolean isActive() {
        return userIdIsActive && tenantIdIsActive;
    }

    /**
     * このユーザーが認証可能かを検証する。
     * ユーザーの有効性（{@link #isActive()}）・パスワード一致をこの順に確認し、
     * いずれかを満たさない場合は {@link AuthenticationFailedException} をスローする。
     *
     * @param rawPassword      照合する生パスワード
     * @param passwordVerifier パスワード照合を行うドメインポート
     * @throws AuthenticationFailedException アカウント無効・テナント無効・パスワード不一致のいずれかの場合
     */
    public void authenticate(RawPassword rawPassword, PasswordVerifier passwordVerifier) {
        if (!isActive()) {
            throw new AuthenticationFailedException();
        }
        if (!passwordVerifier.matches(rawPassword, passwordHash)) {
            throw new AuthenticationFailedException();
        }
    }
}

