package io.github.aki0057.multitenant.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aki0057.multitenant.auth.domain.model.AuthenticatedUser;
import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.Email;
import io.github.aki0057.multitenant.auth.domain.model.vo.PasswordHash;
import io.github.aki0057.multitenant.auth.domain.model.vo.Role;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantCode;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantId;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link JwtAccessTokenVerifier} のユニットテスト。
 *
 * <p>技術アダプタ（ドメインポート実装）のため Spring コンテキストは起動せず、
 * {@link JwtProperties} を直接インスタンス化し、{@link JwtAccessTokenProvider} と
 * {@link JwtAccessTokenVerifier} をコンストラクタで生成して往復検証する。</p>
 */
class JwtAccessTokenVerifierTest {

    /** HS256 に必要な 256bit（32byte）以上を満たすテスト用秘密鍵。 */
    private static final String SECRET = "test-secret-key-for-hs256-which-is-long-enough";

    /** 署名不正の検証に用いる、{@link #SECRET} とは異なる秘密鍵。 */
    private static final String OTHER_SECRET = "another-secret-key-for-hs256-also-long-enough";

    private static final Duration EXPIRATION = Duration.ofMinutes(15);

    private JwtProperties properties;
    private JwtAccessTokenProvider provider;
    private JwtAccessTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties(SECRET, EXPIRATION);
        provider = new JwtAccessTokenProvider(properties);
        verifier = new JwtAccessTokenVerifier(properties);
    }

    private User newUser() {
        return new User(
                new UserId(42L),
                new TenantId(7L),
                new TenantCode("testTenant"),
                new Email("test@example.com"),
                new PasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"),
                new Role("ADMIN"),
                true,
                true
        );
    }

    private SecretKey keyOf(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------
    // 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: 発行したトークンを検証すると userId・tenantId・role が発行時の値と一致する。")
    void verify_validToken() {
        String token = provider.issue(newUser());

        AuthenticatedUser result = verifier.verify(token);

        assertThat(result.userId()).isEqualTo(new UserId(42L));
        assertThat(result.tenantId()).isEqualTo(new TenantId(7L));
        assertThat(result.role()).isEqualTo(new Role("ADMIN"));
    }

    // ---------------------------------------------------------------
    // 異常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("異常系: 別の秘密鍵で署名されたトークンを検証すると SignatureException が伝播する。")
    void verify_signedWithDifferentKey() {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject("42")
                .claim("tenantId", 7L)
                .claim("role", "ADMIN")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(EXPIRATION)))
                .signWith(keyOf(OTHER_SECRET))
                .compact();

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("異常系: 有効期限切れのトークンを検証すると ExpiredJwtException が伝播する。")
    void verify_expiredToken() {
        Instant past = Instant.now().minus(Duration.ofHours(1));
        String token = Jwts.builder()
                .subject("42")
                .claim("tenantId", 7L)
                .claim("role", "ADMIN")
                .issuedAt(Date.from(past.minus(EXPIRATION)))
                .expiration(Date.from(past))
                .signWith(keyOf(SECRET))
                .compact();

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("異常系: 不正フォーマット文字列を検証すると MalformedJwtException が伝播する。")
    void verify_malformedToken() {
        assertThatThrownBy(() -> verifier.verify("not-a-valid-jwt-token"))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("異常系: alg=none の未署名トークンを検証すると UnsupportedJwtException が伝播する。")
    void verify_unsignedToken() {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject("42")
                .claim("tenantId", 7L)
                .claim("role", "ADMIN")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(EXPIRATION)))
                .compact();

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(UnsupportedJwtException.class);
    }
}
