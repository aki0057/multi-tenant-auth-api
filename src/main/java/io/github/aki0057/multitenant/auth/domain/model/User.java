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
     * このユーザーが認証可能かを検証する。
     * アカウント有効性・テナント有効性・パスワード一致をこの順に確認し、
     * いずれかを満たさない場合は {@link AuthenticationFailedException} をスローする。
     *
     * @param rawPassword      照合する生パスワード
     * @param passwordVerifier パスワード照合を行うドメインポート
     * @throws AuthenticationFailedException アカウント無効・テナント無効・パスワード不一致のいずれかの場合
     */
    public void authenticate(RawPassword rawPassword, PasswordVerifier passwordVerifier) {
        if (!userIdIsActive) {
            throw new AuthenticationFailedException();
        }
        if (!tenantIdIsActive) {
            throw new AuthenticationFailedException();
        }
        if (!passwordVerifier.matches(rawPassword, passwordHash)) {
            throw new AuthenticationFailedException();
        }
    }
}

