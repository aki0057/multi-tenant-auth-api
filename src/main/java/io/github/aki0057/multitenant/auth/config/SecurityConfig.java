package io.github.aki0057.multitenant.auth.config;

import io.github.aki0057.multitenant.auth.filter.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST APIはCSRF不要
                .csrf(AbstractHttpConfigurer::disable)

                // REST APIはセッションを使わない
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
                        .requestMatchers("/login").permitAll()                          // loginは認証不要
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // Swaggerは認証不要
                        .anyRequest().authenticated() //規定していないリクエストは全て拒否する
                )

                // JWTフィルターをUsernamePasswordAuthenticationFilterの前に挿入
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}