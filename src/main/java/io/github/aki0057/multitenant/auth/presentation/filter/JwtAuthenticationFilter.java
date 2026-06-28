package io.github.aki0057.multitenant.auth.presentation.filter;

import io.github.aki0057.multitenant.auth.domain.model.AuthenticatedUser;
import io.github.aki0057.multitenant.auth.domain.service.AccessTokenVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT アクセストークンを検証し、SecurityContext に認証情報をセットするフィルター。
 *
 * <p>リクエストごとに 1 回だけ実行される。{@code Authorization: Bearer <token>} ヘッダから
 * トークンを取り出し、{@link AccessTokenVerifier#verify(String)} で検証して
 * {@link AuthenticatedUser} を復元し、{@link UsernamePasswordAuthenticationToken} として
 * SecurityContext に載せる。</p>
 *
 * <p>次の場合は認証をセットせずに後続フィルターへ素通りさせる。認可は {@code SecurityConfig} の
 * {@code authenticated()} + entryPoint による 401 に委ねる。</p>
 * <ul>
 *   <li>{@code Authorization} ヘッダが存在しない、または {@code Bearer } で始まらない場合</li>
 *   <li>{@link AccessTokenVerifier#verify(String)} が例外をスローした場合（署名不正・期限切れ・パース失敗等）</li>
 * </ul>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** {@code Authorization} ヘッダのトークン接頭辞。 */
    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenVerifier accessTokenVerifier;

    /**
     * トークン検証ポートを受け取り、フィルターを構築する。
     *
     * @param accessTokenVerifier JWT アクセストークンを検証するドメインポート
     */
    public JwtAuthenticationFilter(AccessTokenVerifier accessTokenVerifier) {
        this.accessTokenVerifier = accessTokenVerifier;
    }

    /**
     * {@code Authorization: Bearer <token>} ヘッダからトークンを抽出し、検証結果を
     * SecurityContext にセットする。検証できない場合は認証をセットせず素通りさせる。
     *
     * @param request     HTTP リクエスト
     * @param response    HTTP レスポンス
     * @param filterChain 後続のフィルターチェーン
     * @throws ServletException フィルター処理でサーブレット例外が発生した場合
     * @throws IOException      入出力例外が発生した場合
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                AuthenticatedUser authenticatedUser = accessTokenVerifier.verify(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                authenticatedUser,
                                null,
                                List.of(new SimpleGrantedAuthority(
                                        "ROLE_" + authenticatedUser.role().value())));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (RuntimeException e) {
                // 検証失敗（署名不正・期限切れ・パース失敗等）は認証をセットせず素通りさせる。
                // 認可は SecurityConfig の authenticated() + entryPoint による 401 に委ねる。
            }
        }

        filterChain.doFilter(request, response);
    }
}
