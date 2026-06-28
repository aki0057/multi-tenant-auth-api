package io.github.aki0057.multitenant.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.Email;
import io.github.aki0057.multitenant.auth.domain.model.vo.PasswordHash;
import io.github.aki0057.multitenant.auth.domain.model.vo.Role;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantCode;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantId;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link JwtAccessTokenProvider} のユニットテスト。
 *
 * <p>技術アダプタ（ドメインポート実装）のため Spring コンテキストは起動せず、
 * {@link JwtProperties} を直接インスタンス化してコンストラクタで生成する。</p>
 */
class JwtAccessTokenProviderTest {

    /** HS256 に必要な 256bit（32byte）以上を満たすテスト用秘密鍵。 */
    private static final String SECRET = "test-secret-key-for-hs256-which-is-long-enough";

    private static final Duration EXPIRATION = Duration.ofMinutes(15);

    private JwtProperties properties;
    private JwtAccessTokenProvider provider;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties(SECRET, EXPIRATION);
        provider = new JwtAccessTokenProvider(properties);
    }

    private User newUser() {
        return new User(
                new UserId(42L),
                new TenantId(7L),
                new TenantCode("testTenant"),
                new Email("test@example.com"),
                new PasswordHash("hashed-pass"),
                new Role("ADMIN"),
                true,
                true
        );
    }

    private Claims parse(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ---------------------------------------------------------------
    // 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: issue は非 null・非空の JWT 文字列を返す。")
    void issue_returnsNonEmptyToken() {
        String token = provider.issue(newUser());

        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("正常系: 発行された JWT の sub・tenantId・role クレームが期待値と一致する。")
    void issue_claimsMatchUser() {
        String token = provider.issue(newUser());

        Claims claims = parse(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("tenantId", Long.class)).isEqualTo(7L);
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("正常系: exp が iat に expiration を加算した時刻と一致する。")
    void issue_expirationEqualsIssuedAtPlusExpiration() {
        String token = provider.issue(newUser());

        Claims claims = parse(token);
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();

        assertThat(issuedAt).isNotNull();
        assertThat(expiration).isNotNull();
        // JWT の時刻は秒精度に丸められるため、差分は expiration（秒）と一致する。
        long diffSeconds = (expiration.getTime() - issuedAt.getTime()) / 1000;
        assertThat(diffSeconds).isEqualTo(EXPIRATION.getSeconds());
    }

    // ---------------------------------------------------------------
    // 異常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("異常系: user が null の場合は NullPointerException がスローされる。")
    void issue_userIsNull() {
        assertThatThrownBy(() -> provider.issue(null))
                .isInstanceOf(NullPointerException.class);
    }
}
