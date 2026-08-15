package io.github.aki0057.multitenant.auth.presentation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 同一テナントのユーザー一覧の 1 要素を返却するレスポンス DTO。
 * ユーザーの ID・メールアドレス・ロール・有効状態を JSON として返す。
 *
 * <p>無効ユーザーも一覧に含まれ、有効・無効の状態は {@code isActive} で表す。</p>
 *
 * @param id       ユーザーの ID
 * @param email    ユーザーのメールアドレス
 * @param role     ユーザーのロール
 * @param isActive ユーザーが有効かどうか
 */
public record TenantUserResponse(
        @Schema(description = "ユーザーの ID", example = "1")
        Long id,
        @Schema(description = "ユーザーのメールアドレス", example = "user@example.com")
        String email,
        @Schema(description = "ユーザーのロール", example = "USER")
        String role,
        @Schema(description = "ユーザーが有効かどうか", example = "true")
        boolean isActive) {}
