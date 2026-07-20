package io.github.aki0057.multitenant.auth;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /auth/logout} の結合テスト（横断）。
 * presentation → application → domain → infrastructure（H2）までフルコンテキストで起動し、
 * HTTP 入口から DB のリフレッシュトークン失効（{@code refresh_tokens.is_revoked = true}）までを
 * 一気通貫で検証する。
 *
 * <p>検証観点は次のとおり。</p>
 * <ul>
 *   <li>正常系: 有効なリフレッシュトークンと CSRF トークンを伴うと 204 No Content・
 *       {@code refreshToken} 削除クッキー付与・DB の該当行が失効する。</li>
 *   <li>冪等性: {@code refreshToken} クッキー未提示・DB 未一致の未知トークンでも 204 を返し、
 *       例外を投げない。</li>
 *   <li>CSRF 保護: {@code X-XSRF-TOKEN} ヘッダ / {@code XSRF-TOKEN} クッキーが無い・不一致だと
 *       403 Forbidden を返す。</li>
 * </ul>
 *
 * <p>{@code token_hash} は生トークンの SHA-256 で決まるため、有効なリフレッシュトークンは
 * {@code JdbcTemplate} の直接 INSERT では用意できない。よって、先に {@code POST /auth/login} を
 * MockMvc で実行し、実クッキー（{@code refreshToken} / {@code XSRF-TOKEN}）を取得する方式を用いる。</p>
 *
 * <p>テストデータ（tenants / users）は {@code @BeforeEach} で {@code JdbcTemplate} により INSERT し、
 * クラスに付与した {@code @Transactional} により各テスト終了時にロールバックする。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LogoutIntegrationTest {

    private static final String TENANT_CODE = "testTenant";
    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "password";

    /** リフレッシュトークンを運ぶ Cookie 名。 */
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    /** CSRF トークンを運ぶ Cookie 名（{@code CookieCsrfTokenRepository.withHttpOnlyFalse()} 方式）。 */
    private static final String CSRF_COOKIE = "XSRF-TOKEN";

    /** CSRF トークンを送り返すヘッダ名。 */
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    private static final String LOGIN_BODY = """
            {
              "tenantCode": "%s",
              "email": "%s",
              "password": "%s"
            }
            """.formatted(TENANT_CODE, EMAIL, PASSWORD);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * JPA 永続化コンテキスト。
     * {@code AuthService#logout} による revoke（既存エンティティの UPDATE）は
     * テストトランザクション内では flush されるまで DB に反映されない。
     * {@code JdbcTemplate} で直読みする前に明示的に {@code flush()} して反映させるために用いる。
     */
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        OffsetDateTime now = OffsetDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO tenants (code, name, is_active, created_at, updated_at, created_by, updated_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                TENANT_CODE, "テストテナント", true, now, now, "system", "system");

        Long tenantId = jdbcTemplate.queryForObject(
                "SELECT id FROM tenants WHERE code = ?", Long.class, TENANT_CODE);

        // テスト実行時に BCrypt ハッシュを生成する
        String hash = passwordEncoder.encode(PASSWORD);
        jdbcTemplate.update(
                "INSERT INTO users (tenant_id, email, password_hash, role, is_active, created_at, updated_at, created_by, updated_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                tenantId, EMAIL, hash,
                "USER", true, now, now, "system", "system");
    }

    @Test
    @DisplayName("正常系: 有効な refreshToken と CSRF トークンを伴うと 204・refreshToken 削除クッキー付与・DB の該当行が失効する")
    void logout_withValidRefreshTokenAndCsrf_returns204() throws Exception {
        MvcResult loginResult = login();
        Cookie refreshCookie = loginResult.getResponse().getCookie(REFRESH_TOKEN_COOKIE);
        Cookie csrfCookie = loginResult.getResponse().getCookie(CSRF_COOKIE);
        assertThat(refreshCookie).isNotNull();
        assertThat(csrfCookie).isNotNull();

        // ログイン直後は該当行が有効（is_revoked = false）であることを前提として確認する
        assertThat(isRevoked()).isFalse();

        mockMvc.perform(post("/auth/logout")
                        .cookie(refreshCookie, csrfCookie)
                        .header(CSRF_HEADER, csrfCookie.getValue()))
                .andExpect(status().isNoContent())
                // refreshToken を Max-Age=0 で失効させる削除クッキーが付与される
                .andExpect(cookie().maxAge(REFRESH_TOKEN_COOKIE, 0));

        // DB の該当リフレッシュトークンが失効している
        assertThat(isRevoked()).isTrue();
    }

    @Test
    @DisplayName("正常系: refreshToken クッキー未提示でも CSRF トークンが有効なら 204 を返す（冪等）")
    void logout_withoutRefreshTokenCookie_returns204() throws Exception {
        MvcResult loginResult = login();
        Cookie csrfCookie = loginResult.getResponse().getCookie(CSRF_COOKIE);
        assertThat(csrfCookie).isNotNull();

        mockMvc.perform(post("/auth/logout")
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER, csrfCookie.getValue()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("正常系: DB 未一致の未知の refreshToken でも 204 を返し、既存トークンは失効しない（冪等・エラーにしない）")
    void logout_withUnknownRefreshToken_returns204() throws Exception {
        MvcResult loginResult = login();
        Cookie csrfCookie = loginResult.getResponse().getCookie(CSRF_COOKIE);
        assertThat(csrfCookie).isNotNull();

        // DB のどのハッシュにも一致しない未知の生トークン
        Cookie unknownRefreshCookie = new Cookie(REFRESH_TOKEN_COOKIE, "unknown-nonexistent-refresh-token");

        mockMvc.perform(post("/auth/logout")
                        .cookie(unknownRefreshCookie, csrfCookie)
                        .header(CSRF_HEADER, csrfCookie.getValue()))
                .andExpect(status().isNoContent());

        // 未知トークンの失効要求では、ログインで発行された既存トークンは失効しない
        assertThat(isRevoked()).isFalse();
    }

    @Test
    @DisplayName("異常系: CSRF トークン（X-XSRF-TOKEN ヘッダ）が無いと 403 Forbidden が返る")
    void logout_withoutCsrfToken_returns403() throws Exception {
        MvcResult loginResult = login();
        Cookie refreshCookie = loginResult.getResponse().getCookie(REFRESH_TOKEN_COOKIE);
        Cookie csrfCookie = loginResult.getResponse().getCookie(CSRF_COOKIE);
        assertThat(refreshCookie).isNotNull();
        assertThat(csrfCookie).isNotNull();

        // XSRF-TOKEN クッキーはあるが X-XSRF-TOKEN ヘッダを付与しない
        mockMvc.perform(post("/auth/logout")
                        .cookie(refreshCookie, csrfCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("異常系: CSRF トークンがクッキーとヘッダで不一致だと 403 Forbidden が返る")
    void logout_withMismatchedCsrfToken_returns403() throws Exception {
        MvcResult loginResult = login();
        Cookie refreshCookie = loginResult.getResponse().getCookie(REFRESH_TOKEN_COOKIE);
        Cookie csrfCookie = loginResult.getResponse().getCookie(CSRF_COOKIE);
        assertThat(refreshCookie).isNotNull();
        assertThat(csrfCookie).isNotNull();

        // クッキーの値と一致しないヘッダ値を送る
        mockMvc.perform(post("/auth/logout")
                        .cookie(refreshCookie, csrfCookie)
                        .header(CSRF_HEADER, "mismatched-invalid-csrf-token"))
                .andExpect(status().isForbidden());
    }

    /**
     * {@code POST /auth/login} を実行し、実クッキー（{@code refreshToken} / {@code XSRF-TOKEN}）を
     * 含むレスポンスを取得する。有効なリフレッシュトークンおよび CSRF トークンの供給源として用いる。
     *
     * @return ログインの {@link MvcResult}（200 OK）
     * @throws Exception MockMvc 実行時の例外
     */
    private MvcResult login() throws Exception {
        return mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * テストユーザーに紐づくリフレッシュトークン行の {@code is_revoked} を取得する。
     * ログインは 1 件のみリフレッシュトークンを新規発行するため、単一行を前提とする。
     *
     * @return 該当行の {@code is_revoked} 値
     */
    private boolean isRevoked() {
        // JPA 永続化コンテキストの未反映変更（revoke の UPDATE 等）を DB へ書き出してから直読みする
        entityManager.flush();
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, EMAIL);
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT is_revoked FROM refresh_tokens WHERE user_id = ?", Boolean.class, userId));
    }
}
