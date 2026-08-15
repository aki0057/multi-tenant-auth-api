package io.github.aki0057.multitenant.auth.application;

/**
 * 同一テナントのユーザー一覧取得ユースケースへの入力値を表すコマンドオブジェクト。
 * HTTP・認証フレームワークの知識を持たない純粋なデータであり、
 * 外側（presentation）との境界を表す DTO のためプリミティブ型を保持する。
 * プリミティブから Value Object への変換は
 * {@link UserService#listTenantUsers(ListTenantUsersCommand)} の入口で行う。
 *
 * <p>ADMIN ロールかどうかの認可判定は {@code SecurityConfig} 側に寄せるため、
 * 本コマンドはロール・ユーザー ID を運ばない。</p>
 *
 * @param tenantId 一覧取得の対象テナントの主キー値（アクセストークンの {@code tenantId} クレーム由来）
 */
public record ListTenantUsersCommand(Long tenantId) {}
