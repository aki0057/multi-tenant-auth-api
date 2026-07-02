package io.github.aki0057.multitenant.auth.presentation;

import io.github.aki0057.multitenant.auth.application.AuthService;
import io.github.aki0057.multitenant.auth.application.LoginCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * ログインエンドポイント。
     * 認証情報を検証し、成功した場合は JWT アクセストークンを JSON で返す。
     *
     * @param request ログインリクエスト（テナントコード・メールアドレス・パスワード）
     * @return 発行された JWT アクセストークンとトークン種別を含む {@link LoginResponse}（200 OK）
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String accessToken = authService.login(new LoginCommand(
                request.getTenantCode(),
                request.getEmail(),
                request.getPassword()
        ));
        return ResponseEntity.ok(new LoginResponse(accessToken, "Bearer"));
    }
}
