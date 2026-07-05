package io.github.aki0057.multitenant.auth.presentation;

import io.github.aki0057.multitenant.auth.application.AuthService;
import io.github.aki0057.multitenant.auth.application.LoginCommand;
import io.github.aki0057.multitenant.auth.application.LoginResult;
import io.github.aki0057.multitenant.auth.application.RefreshCommand;
import io.github.aki0057.multitenant.auth.application.RefreshResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    /** リフレッシュトークンを運ぶ Cookie 名。 */
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;

    /**
     * CSRF トークンの保存先リポジトリ（{@code XSRF-TOKEN} クッキー方式）。
     * {@code /refresh} チェーンの CSRF 検証と共有し、login レスポンス時点での
     * {@code XSRF-TOKEN} 先行発行に用いる。
     */
    private final CsrfTokenRepository csrfTokenRepository;

    /**
     * ログインエンドポイント。
     * 認証情報を検証し、成功した場合は JWT アクセストークンを発行するとともに
     * リフレッシュトークンを新規発行して {@code Set-Cookie}
     * （{@code refreshToken; HttpOnly; Secure; SameSite=Strict; Path=/refresh}）で返す。
     * リフレッシュトークンは JSON ボディには含めない。
     * <p>
     * あわせて、後続の {@code POST /refresh} が CSRF トークンを送信できるよう、
     * このレスポンス時点で {@code XSRF-TOKEN} クッキーを発行する（鶏卵問題の回避）。
     *
     * @param request      ログインリクエスト（テナントコード・メールアドレス・パスワード）
     * @param httpRequest  {@code XSRF-TOKEN} 発行のためのサーブレットリクエスト
     * @param httpResponse {@code XSRF-TOKEN} クッキー書き出しのためのサーブレットレスポンス
     * @return 発行された JWT アクセストークンとトークン種別を含む
     *         {@link LoginResponse}（200 OK）。リフレッシュトークンと {@code XSRF-TOKEN}
     *         は {@code Set-Cookie} で返す。
     */
    @Operation(
            summary = "ログイン",
            description = "テナントコード・メールアドレス・パスワードで認証し、成功時に JWT アクセストークンを発行する。"
                    + "リフレッシュトークンは HttpOnly な Set-Cookie（refreshToken）で返し、あわせて XSRF-TOKEN "
                    + "クッキーを先行発行する。認証不要（permitAll）のエンドポイント。")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        LoginResult result = authService.login(new LoginCommand(
                request.getTenantCode(),
                request.getEmail(),
                request.getPassword()
        ));

        // login は CSRF 無効チェーンに属し CSRF フィルターが動かないため、
        // ここで XSRF-TOKEN クッキーを明示的に発行する（初回 POST /refresh の 403 を回避）。
        CsrfToken csrfToken = csrfTokenRepository.generateToken(httpRequest);
        csrfTokenRepository.saveToken(csrfToken, httpRequest, httpResponse);

        ResponseCookie refreshCookie = buildRefreshTokenCookie(result.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new LoginResponse(result.accessToken(), "Bearer"));
    }

    /**
     * リフレッシュエンドポイント。
     * Cookie で提示されたリフレッシュトークンを検証し、成功した場合はアクセストークンを
     * 再発行するとともに、ローテーション後の新しいリフレッシュトークンを {@code Set-Cookie}
     * （{@code refreshToken; HttpOnly; Secure; SameSite=Strict; Path=/refresh}）で返す。
     * リフレッシュトークンは JSON ボディには含めない。
     *
     * @param refreshToken {@code refreshToken} クッキーで受け取ったリフレッシュトークン
     * @return 再発行されたアクセストークンとトークン種別を含む
     *         {@link RefreshResponse}（200 OK）。新しいリフレッシュトークンは
     *         {@code Set-Cookie} で返す。
     */
    @Operation(
            summary = "アクセストークンのリフレッシュ",
            description = "refreshToken クッキーで提示されたリフレッシュトークンを検証し、アクセストークンを再発行する。"
                    + "リフレッシュトークンはローテーションされ、新しい値を HttpOnly な Set-Cookie（refreshToken）で返す。"
                    + "この経路は CSRF 保護有効のため、XSRF-TOKEN クッキーの値を X-XSRF-TOKEN ヘッダで送り返す必要がある。"
                    + "認証不要（permitAll）のエンドポイント。")
    @Parameter(
            name = REFRESH_TOKEN_COOKIE,
            in = ParameterIn.COOKIE,
            description = "ログイン時に発行されたリフレッシュトークン（refreshToken クッキー）")
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @CookieValue(REFRESH_TOKEN_COOKIE) String refreshToken) {
        RefreshResult result = authService.refresh(new RefreshCommand(refreshToken));

        ResponseCookie refreshCookie = buildRefreshTokenCookie(result.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new RefreshResponse(result.accessToken(), "Bearer"));
    }

    /**
     * リフレッシュトークンを運ぶ {@code Set-Cookie} を構築する。
     * {@code HttpOnly; Secure; SameSite=Strict; Path=/refresh} を付与し、
     * JavaScript からの参照と {@code /refresh} 以外への送信を防ぐ。
     *
     * @param rawToken Cookie に載せる生のリフレッシュトークン
     * @return 構築した {@link ResponseCookie}
     */
    private ResponseCookie buildRefreshTokenCookie(String rawToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, rawToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/refresh")
                .build();
    }
}
