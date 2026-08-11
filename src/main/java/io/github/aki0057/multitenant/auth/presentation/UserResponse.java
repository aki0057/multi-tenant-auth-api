package io.github.aki0057.multitenant.auth.presentation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ログイン中ユーザー自身の情報を返却するレスポンス DTO。
 * メールアドレスとロールを JSON として返す。
 *
 * @param email ログイン中ユーザーのメールアドレス
 * @param role  ログイン中ユーザーのロール（{@code "ADMIN"} または {@code "USER"}）
 */
public record UserResponse(
        @Schema(description = "ログイン中ユーザーのメールアドレス", example = "user@example.com")
        String email,
        @Schema(description = "ログイン中ユーザーのロール", example = "USER")
        String role) {}
