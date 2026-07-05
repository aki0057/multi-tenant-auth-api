package io.github.aki0057.multitenant.auth.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    @Schema(description = "テナントコード（英数字）", example = "acme", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "tenantCode は必須です")
    private String tenantCode;

    @Schema(description = "ユーザーのメールアドレス", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "email は必須です")
    private String email;

    @Schema(description = "パスワード", example = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "password は必須です")
    private String password;
}

