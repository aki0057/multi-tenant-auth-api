package io.github.aki0057.multitenant.auth.application;

/**
 * ログアウトユースケースへの入力値を表すコマンドオブジェクト。
 * HTTP・バリデーションの知識を持たない純粋なデータ。
 *
 * @param refreshToken クライアントから提示されたリフレッシュトークン（未提示の場合は {@code null}）
 */
public record LogoutCommand(String refreshToken) {}
