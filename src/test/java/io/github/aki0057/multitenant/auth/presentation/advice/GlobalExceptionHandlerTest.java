package io.github.aki0057.multitenant.auth.presentation.advice;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link GlobalExceptionHandler} のスタンドアローンテスト。
 * Spring コンテキストを起動せず、MockMvcBuilders.standaloneSetup() で検証する。
 */
class GlobalExceptionHandlerTest {

    /** テストごとに投げる例外を保持するスレッドセーフなホルダー。 */
    private static final AtomicReference<Exception> EXCEPTION_HOLDER = new AtomicReference<>();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        EXCEPTION_HOLDER.set(null);
    }

    // -------------------------------------------------------------------------
    // BadCredentialsException → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("BadCredentialsException が発生した場合、401 Unauthorized が返る")
    void handleBadCredentials_returns401() throws Exception {
        EXCEPTION_HOLDER.set(new BadCredentialsException("bad credentials"));

        mockMvc.perform(get("/throw"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // MethodArgumentNotValidException → 400
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("MethodArgumentNotValidException が発生した場合、400 Bad Request が返る")
    void handleValidation_returns400() throws Exception {
        EXCEPTION_HOLDER.set(mock(MethodArgumentNotValidException.class));

        mockMvc.perform(get("/throw"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Exception（予期しない例外） → 500
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("予期しない Exception が発生した場合、500 Internal Server Error が返る")
    void handleUnexpected_returns500() throws Exception {
        EXCEPTION_HOLDER.set(new RuntimeException("unexpected error"));

        mockMvc.perform(get("/throw"))
                .andExpect(status().isInternalServerError());
    }

    // -------------------------------------------------------------------------
    // テスト用ダミーコントローラー
    // -------------------------------------------------------------------------

    /**
     * EXCEPTION_HOLDER に格納された例外を投げるだけのダミーコントローラー。
     * GlobalExceptionHandler の各ハンドラーを独立して検証するために使用する。
     */
    @RestController
    static class DummyController {

        @GetMapping("/throw")
        public void throwException() throws Exception {
            throw EXCEPTION_HOLDER.get();
        }
    }
}
