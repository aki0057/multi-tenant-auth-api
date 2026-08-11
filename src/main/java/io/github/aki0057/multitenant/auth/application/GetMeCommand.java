package io.github.aki0057.multitenant.auth.application;

/**
 * ログイン中ユーザー情報取得ユースケースへの入力値を表すコマンドオブジェクト。
 * HTTP・認証フレームワークの知識を持たない純粋なデータであり、
 * 外側（presentation）との境界を表す DTO のためプリミティブ型を保持する。
 * プリミティブから Value Object への変換は {@link UserService#getMe(GetMeCommand)} の入口で行う。
 *
 * @param userId   ユーザーの主キー値（アクセストークンの {@code sub} クレーム由来）
 * @param tenantId テナントの主キー値（アクセストークンの {@code tenantId} クレーム由来）
 */
public record GetMeCommand(Long userId, Long tenantId) {}
