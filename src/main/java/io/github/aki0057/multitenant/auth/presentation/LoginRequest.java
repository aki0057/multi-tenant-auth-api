package io.github.aki0057.multitenant.auth.presentation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "tenantCode は必須です")
    private String tenantCode;

    @NotBlank(message = "email は必須です")
    @Email(message = "email はメールアドレスの形式である必要があります")
    private String email;

    @NotBlank(message = "password は必須です")
    private String password;
}

