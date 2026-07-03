package io.github.aki0057.multitenant.auth.application;

/**
 * リフレッシュユースケースへの入力値を表すコマンドオブジェクト。
 * HTTP・バリデーションの知識を持たない純粋なデータ。
 *
 * @param refreshToken クライアントから提示されたリフレッシュトークン
 */
public record RefreshCommand(String refreshToken) {}
