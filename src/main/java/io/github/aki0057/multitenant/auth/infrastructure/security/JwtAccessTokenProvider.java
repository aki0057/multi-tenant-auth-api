package io.github.aki0057.multitenant.auth.infrastructure.security;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.service.AccessTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * {@link AccessTokenProvider} の実装アダプタ。
 *
 * <p>jjwt（0.13.0）を用いて、認証済みユーザーから HS256 署名済みの
 * JWT アクセストークンを発行する。</p>
 *
 * <p>ペイロード構成:</p>
 * <ul>
 *   <li>{@code sub} — ユーザー ID（{@link User#userId()} の値を文字列化）</li>
 *   <li>{@code iat} — 発行時刻</li>
 *   <li>{@code exp} — {@code iat} に {@link JwtProperties#expiration()} を加算した失効時刻</li>
 *   <li>{@code tenantId}（カスタムクレーム） — テナント ID（{@link Long}）</li>
 *   <li>{@code role}（カスタムクレーム） — ロール（{@link String}）</li>
 * </ul>
 */
@Component
public class JwtAccessTokenProvider implements AccessTokenProvider {

    private final JwtProperties properties;
    private final SecretKey secretKey;

    /**
     * {@link JwtProperties} を受け取り、HS256 署名鍵を構築する。
     *
     * @param properties JWT 設定値（秘密鍵・有効期間）
     */
    public JwtAccessTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 認証済みユーザーから HS256 署名済みの JWT アクセストークンを発行する。
     *
     * @param user 認証済みユーザー
     * @return 署名済み JWT 文字列
     */
    @Override
    public String issue(User user) {
        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plus(properties.expiration());

        return Jwts.builder()
                .subject(String.valueOf(user.userId().value()))
                .claim("tenantId", user.tenantId().value())
                .claim("role", user.role().value())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }
}
