package io.github.aki0057.multitenant.auth.application;

/**
 * ログイン中ユーザー情報取得ユースケースの実行結果を表す出力オブジェクト。
 * 該当ユーザーのメールアドレスとロールを保持する、ロジックを持たない純粋なデータ。
 *
 * @param email 該当ユーザーのメールアドレス
 * @param role  該当ユーザーのロール（{@code "ADMIN"} または {@code "USER"}）
 */
public record GetMeResult(String email, String role) {}
