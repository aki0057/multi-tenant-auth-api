package io.github.aki0057.multitenant.auth.presentation;

import io.github.aki0057.multitenant.auth.application.AuthService;
import io.github.aki0057.multitenant.auth.config.PasswordEncoderConfig;
import io.github.aki0057.multitenant.auth.config.SecurityConfig;
import io.github.aki0057.multitenant.auth.presentation.advice.GlobalExceptionHandler;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private static final String VALID_REQUEST = """
            {
              "tenantCode": "test-tenant",
              "email": "test@example.com",
              "password": "password"
            }
            """;

    private static final String INVALID_REQUEST = """
                {
                  "tenantCode": "",
                  "email": "not-an-email",
                  "password": ""
                }
                """;

    @Test
    @DisplayName("正常系: 正しい認証情報を送信すると 200 OK が返る。")
    void login_ShouldReturn200Ok() throws Exception {
        doNothing().when(authService).login(any());

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("異常系: ユーザーが存在しない / パスワード不一致の場合は 401 が返る。")
    void login_ShouldReturn401_WhenCredentialsAreInvalid() throws Exception {
        doThrow(new BadCredentialsException("dummy")) // 文字列は何でもよい
                .when(authService).login(any());

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("異常系: リクエストボディのバリデーション失敗時は 400 が返る。")
    void login_ShouldReturn400_WhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_REQUEST))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("異常系: 予期しない例外が発生した場合は 500 が返る。")
    void login_ShouldReturn500_WhenUnexpectedExceptionOccurs() throws Exception {
        doThrow(new RuntimeException("dummy"))// 文字列は何でもよい
                .when(authService).login(any());

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isInternalServerError());
    }
}