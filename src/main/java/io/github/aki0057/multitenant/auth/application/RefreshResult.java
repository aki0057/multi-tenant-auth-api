package io.github.aki0057.multitenant.auth.application;

/**
 * リフレッシュユースケースの実行結果を表す出力オブジェクト。
 * 再発行されたアクセストークンと、ローテーション後の新しいリフレッシュトークンを保持する。
 * ロジックを持たない純粋なデータ。
 *
 * @param accessToken  再発行された JWT アクセストークン
 * @param refreshToken ローテーション後の新しいリフレッシュトークン
 */
public record RefreshResult(String accessToken, String refreshToken) {}
