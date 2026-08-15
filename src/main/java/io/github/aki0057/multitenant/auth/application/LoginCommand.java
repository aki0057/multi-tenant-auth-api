package io.github.aki0057.multitenant.auth.application;

/**
 * ログインユースケースへの入力値を表すコマンドオブジェクト。
 * HTTP・バリデーションの知識を持たない純粋なデータ。
 */
public record LoginCommand(
        String tenantCode,
        String email,
        String password
) {}

