package io.github.aki0057.multitenant.auth.application;

/**
 * ログインユースケースの実行結果を表す出力オブジェクト。
 * 発行された JWT アクセストークンと、新規発行されたリフレッシュトークンを保持する。
 * ロジックを持たない純粋なデータ。
 *
 * @param accessToken  発行された JWT アクセストークン
 * @param refreshToken 新規発行されたリフレッシュトークン
 */
public record LoginResult(String accessToken, String refreshToken) {}
