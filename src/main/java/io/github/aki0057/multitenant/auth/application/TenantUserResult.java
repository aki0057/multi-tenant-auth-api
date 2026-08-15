package io.github.aki0057.multitenant.auth.application;

/**
 * 同一テナントのユーザー一覧取得ユースケースの実行結果の 1 要素を表す出力オブジェクト。
 * 該当ユーザーの ID・メールアドレス・ロール・有効状態を保持する、ロジックを持たない純粋なデータ。
 *
 * <p>無効ユーザー（{@code isActive} が {@code false}）もこの結果に含まれる。
 * 管理者が自テナントのユーザー状態を把握する用途のため、
 * 無効ユーザーを 404 相当として扱う {@link UserService#getMe(GetMeCommand)} とは
 * 意図的に方針が異なる。</p>
 *
 * @param id       該当ユーザーの主キー値
 * @param email    該当ユーザーのメールアドレス
 * @param role     該当ユーザーのロール（{@code "ADMIN"} または {@code "USER"}）
 * @param isActive 該当ユーザーが有効かどうか
 */
public record TenantUserResult(Long id, String email, String role, boolean isActive) {}
