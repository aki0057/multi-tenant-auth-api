package io.github.aki0057.multitenant.auth.config;

import io.github.aki0057.multitenant.auth.domain.service.AccessTokenVerifier;
import io.github.aki0057.multitenant.auth.presentation.filter.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(AccessTokenVerifier accessTokenVerifier) {
        return new JwtAuthenticationFilter(accessTokenVerifier);
    }

    /**
     * CSRF トークンの保存先リポジトリ。
     * セッションを持たない（STATELESS）ため、{@code XSRF-TOKEN} クッキー方式
     * （{@link CookieCsrfTokenRepository#withHttpOnlyFalse()}）を用いる。
     * {@code /auth/refresh} チェーンでの CSRF 検証と、{@code AuthController#login} での
     * {@code XSRF-TOKEN} 先行発行の双方で同一インスタンスを共有する。
     *
     * @return {@code XSRF-TOKEN} クッキー方式の CSRF トークンリポジトリ
     */
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    /**
     * {@code /auth/refresh} と {@code /auth/logout} 専用の SecurityFilterChain（高優先）。
     * リフレッシュトークンのローテーション・破棄を保護するため、これらの経路のみ CSRF を有効化する。
     * クライアントは {@code XSRF-TOKEN} クッキーの値を {@code X-XSRF-TOKEN} ヘッダで
     * 送り返す。Swagger UI が送るのは XOR エンコードされていない生の値のため、
     * {@link CsrfTokenRequestAttributeHandler}（平文比較）を用いる。
     *
     * @param http                 Spring Security の HTTP 設定ビルダー
     * @param csrfTokenRepository  共有する CSRF トークンリポジトリ
     * @return {@code /auth/refresh}・{@code /auth/logout} 用の {@link SecurityFilterChain}
     * @throws Exception 設定構築に失敗した場合
     */
    @Bean
    @Order(1)
    public SecurityFilterChain refreshSecurityFilterChain(
            HttpSecurity http, CsrfTokenRepository csrfTokenRepository) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        http
                // このチェーンは /auth/refresh と /auth/logout を対象とする
                .securityMatcher("/auth/refresh", "/auth/logout")

                // /auth/refresh・/auth/logout のみ CSRF 保護を有効化（XSRF-TOKEN クッキー方式）
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(requestHandler)
                )

                // セッションを使わない
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 認証自体は不要（トークン検証はアプリケーション層で行う）
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * デフォルトの SecurityFilterChain（{@code /auth/refresh}、{@code /auth/logout} 以外すべて）。
     * REST API のため CSRF は無効・STATELESS とし、{@code /auth/login} と Swagger 関連の
     * エンドポイントを認証不要にする。JWT フィルターを挿入し、認証・認可エラーは
     * それぞれ 401・404 の JSON で返す。
     *
     * @param http                 Spring Security の HTTP 設定ビルダー
     * @param accessTokenVerifier  JWT フィルターが用いるアクセストークン検証ポート
     * @return デフォルトの {@link SecurityFilterChain}
     * @throws Exception 設定構築に失敗した場合
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http, AccessTokenVerifier accessTokenVerifier) throws Exception {
        http
                // CSRF保護を無効化
                .csrf(AbstractHttpConfigurer::disable)

                // セッションを使わない
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 認証エラー時のレスポンスをJSONで返す
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            // 401 Unauthorizedを返す
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"error\": \"Unauthorized\"}");
                        })
                        .accessDeniedHandler((request, response, e) -> {
                            // 403 Forbiddenではなく404 Not Foundを返す
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            response.getWriter().write("{\"error\": \"Not Found\"}");
                        })
                )

                // エンドポイントの認可設定
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login").permitAll()                       // loginは認証不要
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // Swaggerは認証不要
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")               // 管理者向けAPIはADMINのみ
                        .anyRequest().authenticated() //規定していないリクエストは全て拒否する
                )

                // JWTフィルターをUsernamePasswordAuthenticationFilterの前に挿入
                .addFilterBefore(jwtAuthenticationFilter(accessTokenVerifier), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
