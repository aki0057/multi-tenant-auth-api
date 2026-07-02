package io.github.aki0057.multitenant.auth.presentation.filter;

import io.github.aki0057.multitenant.auth.domain.model.AuthenticatedUser;
import io.github.aki0057.multitenant.auth.domain.model.vo.Role;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantId;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;
import io.github.aki0057.multitenant.auth.domain.service.AccessTokenVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * {@link JwtAuthenticationFilter} の単体テスト。
 *
 * <p>Spring を起動せず、{@link MockHttpServletRequest} / {@link MockHttpServletResponse} /
 * {@link MockFilterChain} でフィルタを直接呼び出し、{@link AccessTokenVerifier} はモック化する。</p>
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private AccessTokenVerifier accessTokenVerifier;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(accessTokenVerifier);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("正常系: 有効な Bearer トークンを検証し SecurityContext に認証がセットされる。")
    void doFilterInternal_validBearerToken() throws Exception {
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(new UserId(1L), new TenantId(2L), new Role("ADMIN"));
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        when(accessTokenVerifier.verify("valid-token")).thenReturn(authenticatedUser);

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(authenticatedUser);
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ADMIN");
        // doFilter が呼ばれたことを、チェーンへ渡されたリクエストで確認する
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("異常系: Authorization ヘッダがない場合は認証をセットせず素通りする。")
    void doFilterInternal_noAuthorizationHeader() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("異常系: Bearer で始まらないヘッダの場合は認証をセットせず素通りする。")
    void doFilterInternal_nonBearerHeader() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("異常系: verify が例外をスローした場合は認証をセットせず素通りする。")
    void doFilterInternal_verifyThrows() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
        when(accessTokenVerifier.verify("invalid-token"))
                .thenThrow(new RuntimeException("署名検証に失敗しました。"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isSameAs(request);
    }
}
