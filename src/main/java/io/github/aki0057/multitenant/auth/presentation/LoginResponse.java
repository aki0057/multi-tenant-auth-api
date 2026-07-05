package io.github.aki0057.multitenant.auth.presentation;

/**
 * ログイン成功時に返却するレスポンス DTO。
 * 発行された JWT アクセストークンと、新規発行されたリフレッシュトークン、
 * およびトークン種別を JSON として返す。
 *
 * @param accessToken  発行された JWT アクセストークン
 * @param refreshToken 新規発行されたリフレッシュトークン
 * @param tokenType    トークン種別（{@code "Bearer"} を固定で返す）
 */
public record LoginResponse(String accessToken, String refreshToken, String tokenType) {}
