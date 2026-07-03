package io.github.aki0057.multitenant.auth.presentation;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * リフレッシュエンドポイントのリクエスト DTO。
 * クライアントが保持するリフレッシュトークンを受け取り、
 * アクセストークンの再発行要求を表現する。
 */
@Getter
@NoArgsConstructor
public class RefreshRequest {

    @NotBlank(message = "refreshToken は必須です")
    private String refreshToken;
}
