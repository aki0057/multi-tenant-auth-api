package io.github.aki0057.multitenant.auth.presentation;

/**
 * リフレッシュ成功時に返却するレスポンス DTO。
 * 再発行された JWT アクセストークンとトークン種別を JSON として返す。
 * ローテーション後の新しいリフレッシュトークンは JSON ボディには含めず、
 * HttpOnly な {@code Set-Cookie}（{@code refreshToken}）でクライアントへ返す。
 *
 * @param accessToken 再発行された JWT アクセストークン
 * @param tokenType   トークン種別（{@code "Bearer"} を固定で返す）
 */
public record RefreshResponse(String accessToken, String tokenType) {}
