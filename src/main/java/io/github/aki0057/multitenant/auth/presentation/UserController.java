package io.github.aki0057.multitenant.auth.presentation;

import io.github.aki0057.multitenant.auth.application.GetMeCommand;
import io.github.aki0057.multitenant.auth.application.UserService;
import io.github.aki0057.multitenant.auth.domain.model.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * ログイン中ユーザー自身の情報を返すエンドポイント。
     * {@code JwtAuthenticationFilter} が SecurityContext にセットした
     * {@link AuthenticatedUser} からユーザー ID・テナント ID を取り出して application 層へ渡し、
     * 該当ユーザーのメールアドレスとロールを JSON で返す。
     *
     * <p>アクセストークンには email クレームを含まないため、メールアドレスは
     * application 層が参照した値をそのまま返す。ロールは principal が保持する
     * {@code Role}（Value Object）をプリミティブな文字列へ変換して返す。</p>
     *
     * <p>未認証・不正／期限切れトークンの場合は本メソッドに到達せず、
     * {@code SecurityConfig} の {@code authenticationEntryPoint} により 401 Unauthorized が返る。</p>
     *
     * @param authenticatedUser アクセストークンから復元された認証済みユーザー
     * @return ログイン中ユーザーのメールアドレスとロールを含む {@link UserResponse}（200 OK）。
     *         該当ユーザーが存在しない場合は本文なしの 404 Not Found
     */
    @Operation(
            summary = "ログイン中ユーザー情報の取得",
            description = "アクセストークンで認証されたユーザー自身のメールアドレスとロールを返す。"
                    + "認証必須（Authorization: Bearer <アクセストークン>）のエンドポイント。"
                    + "該当ユーザーが存在しない場合は 404 を返す。")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/users/me")
    public ResponseEntity<UserResponse> getMe(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return userService.getMe(new GetMeCommand(
                        authenticatedUser.userId().value(),
                        authenticatedUser.tenantId().value()))
                .map(result -> ResponseEntity.ok(new UserResponse(
                        result.email(), authenticatedUser.role().value())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
