package io.github.aki0057.multitenant.auth.presentation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ログイン成功時に返却するレスポンス DTO。
 * 発行された JWT アクセストークンとトークン種別を JSON として返す。
 * リフレッシュトークンは JSON ボディには含めず、HttpOnly な {@code Set-Cookie}
 * （{@code refreshToken}）でクライアントへ返す。
 *
 * @param accessToken 発行された JWT アクセストークン
 * @param tokenType   トークン種別（{@code "Bearer"} を固定で返す）
 */
public record LoginResponse(
        @Schema(description = "発行された JWT アクセストークン", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "トークン種別", example = "Bearer")
        String tokenType) {}
