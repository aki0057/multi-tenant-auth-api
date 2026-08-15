package io.github.aki0057.multitenant.auth.presentation;

import io.github.aki0057.multitenant.auth.application.AuthService;
import io.github.aki0057.multitenant.auth.application.LoginResult;
import io.github.aki0057.multitenant.auth.application.RefreshResult;
import io.github.aki0057.multitenant.auth.config.PasswordEncoderConfig;
import io.github.aki0057.multitenant.auth.config.SecurityConfig;
import io.github.aki0057.multitenant.auth.domain.service.AccessTokenVerifier;
import io.github.aki0057.multitenant.auth.presentation.advice.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    /** SecurityConfig#defaultSecurityFilterChain が要求する依存を満たすためのモック。 */
    @MockitoBean
    private AccessTokenVerifier accessTokenVerifier;

    private static final String VALID_REQUEST = """
            {
              "tenantCode": "testTenant",
              "email": "test@example.com",
              "password": "password"
            }
            """;

    private static final String INVALID_REQUEST = """
                {
                  "tenantCode": "",
                  "email": "",
                  "password": ""
                }
                """;

    @Test
    @DisplayName("正常系: 正しい認証情報を送信すると 200 OK・アクセストークンが返り、リフレッシュトークンは Cookie で返る。")
    void login_success() throws Exception {
        when(authService.login(any()))
                .thenReturn(new LoginResult("mock-access-token", "mock-refresh-token"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                // リフレッシュトークンは JSON ボディに含めない
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                // リフレッシュトークンは属性付きの Set-Cookie で返る
                .andExpect(cookie().value("refreshToken", "mock-refresh-token"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().secure("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/auth"))
                .andExpect(cookie().sameSite("refreshToken", "Strict"));
    }

    @Test
    @DisplayName("正常系: login レスポンスの Set-Cookie に XSRF-TOKEN が発行される（後続 /auth/refresh の鶏卵問題回避）。")
    void login_issuesXsrfTokenCookie() throws Exception {
        when(authService.login(any()))
                .thenReturn(new LoginResult("mock-access-token", "mock-refresh-token"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    @DisplayName("異常系: ユーザーが存在しない / パスワード不一致の場合は 401 が返る。")
    void login_invalidCredentials() throws Exception {
        doThrow(new BadCredentialsException("dummy")) // 文字列は何でもよい
                .when(authService).login(any());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("異常系: リクエストボディのバリデーション失敗時は 400 が返る。")
    void login_invalidRequest() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_REQUEST))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("異常系: 予期しない例外が発生した場合は 500 が返る。")
    void login_unexpectedException() throws Exception {
        doThrow(new RuntimeException("dummy")) // 文字列は何でもよい
                .when(authService).login(any());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("正常系: Cookie のリフレッシュトークンを送信すると 200 OK・アクセストークンが返り、新トークンが Cookie で返る。")
    void refresh_success() throws Exception {
        when(authService.refresh(any()))
                .thenReturn(new RefreshResult("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", "old-refresh-token"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                // リフレッシュトークンは JSON ボディに含めない
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                // ローテーション後トークンは属性付きの Set-Cookie で返る
                .andExpect(cookie().value("refreshToken", "new-refresh-token"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().secure("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/auth"))
                .andExpect(cookie().sameSite("refreshToken", "Strict"));
    }

    @Test
    @DisplayName("異常系: refreshToken Cookie 未送付時は 401 が返る（500 にしない）。")
    void refresh_missingCookie() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("異常系: CSRF トークン（X-XSRF-TOKEN）なしで /auth/refresh へ POST すると 403 が返る。")
    void refresh_missingCsrfToken() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", "old-refresh-token")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("正常系: refreshToken Cookie を送信すると 204 が返り、refreshToken / XSRF-TOKEN が Max-Age=0 で失効する。")
    void logout_success() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refreshToken", "old-refresh-token"))
                        .with(csrf()))
                .andExpect(status().isNoContent())
                // refreshToken は Max-Age=0・発行時と同一属性で削除される
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().secure("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/auth"))
                .andExpect(cookie().sameSite("refreshToken", "Strict"))
                // XSRF-TOKEN も Max-Age=0 で削除される（JS 参照可のため HttpOnly=false）
                .andExpect(cookie().maxAge("XSRF-TOKEN", 0))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));

        verify(authService).logout(any());
    }

    @Test
    @DisplayName("正常系: refreshToken Cookie 未送付でも 204 が返る（冪等）。")
    void logout_missingCookie() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(cookie().maxAge("XSRF-TOKEN", 0));

        verify(authService).logout(any());
    }

    @Test
    @DisplayName("異常系: CSRF トークン（X-XSRF-TOKEN）なしで /auth/logout へ POST すると 403 が返る。")
    void logout_missingCsrfToken() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refreshToken", "old-refresh-token")))
                .andExpect(status().isForbidden());
    }
}
