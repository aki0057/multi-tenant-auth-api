package io.github.aki0057.multitenant.auth.presentation;

/**
 * リフレッシュ成功時に返却するレスポンス DTO。
 * 再発行された JWT アクセストークンと、ローテーション後の新しいリフレッシュトークン、
 * およびトークン種別を JSON として返す。
 *
 * @param accessToken  再発行された JWT アクセストークン
 * @param refreshToken ローテーション後の新しいリフレッシュトークン
 * @param tokenType    トークン種別（{@code "Bearer"} を固定で返す）
 */
public record RefreshResponse(String accessToken, String refreshToken, String tokenType) {}
