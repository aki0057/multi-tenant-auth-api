package io.github.aki0057.multitenant.auth.infrastructure.security;

import io.github.aki0057.multitenant.auth.domain.model.AuthenticatedUser;
import io.github.aki0057.multitenant.auth.domain.model.vo.Role;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantId;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;
import io.github.aki0057.multitenant.auth.domain.service.AccessTokenVerifier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * {@link AccessTokenVerifier} の実装アダプタ。
 *
 * <p>jjwt（0.13.0）を用いて、HS256 署名済みの JWT アクセストークンを検証し、
 * クレームから {@link AuthenticatedUser} を復元する。発行側 {@link JwtAccessTokenProvider}
 * と同一の {@link JwtProperties} / 鍵生成方法を共有することで、発行・検証で署名鍵を一致させる。</p>
 *
 * <p>検証内容:</p>
 * <ul>
 *   <li>HS256 署名の正当性（{@code verifyWith} に渡した秘密鍵との一致）</li>
 *   <li>有効期限（{@code exp} クレーム）の超過有無</li>
 *   <li>{@code parseSignedClaims} を用いることで、{@code alg=none} の未署名トークンは
 *       {@code UnsupportedJwtException} で自動的に拒否される</li>
 * </ul>
 *
 * <p>ペイロードと {@link AuthenticatedUser} の対応:</p>
 * <ul>
 *   <li>{@code sub} — ユーザー ID（{@link UserId}）。{@code Long.parseLong} で復元する</li>
 *   <li>{@code tenantId}（カスタムクレーム） — テナント ID（{@link TenantId}）</li>
 *   <li>{@code role}（カスタムクレーム） — ロール（{@link Role}）</li>
 * </ul>
 *
 * <p>jjwt がスローする例外（{@code SignatureException} / {@code ExpiredJwtException} /
 * {@code MalformedJwtException} / {@code UnsupportedJwtException} 等）は try/catch せず、
 * そのまま呼び出し元（フィルタ側）へ伝播させる。</p>
 */
@Component
public class JwtAccessTokenVerifier implements AccessTokenVerifier {

    private final SecretKey secretKey;

    /**
     * {@link JwtProperties} を受け取り、HS256 署名鍵を構築する。
     *
     * <p>鍵生成方法は発行側 {@link JwtAccessTokenProvider} をミラーし、発行・検証で
     * 同一の {@link SecretKey} を用いる。</p>
     *
     * @param properties JWT 設定値（秘密鍵・有効期間）
     */
    public JwtAccessTokenVerifier(JwtProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JWT アクセストークンを検証し、クレームから認証情報を復元する。
     *
     * @param token 検証対象の JWT アクセストークン
     * @return トークンのクレームから復元した認証情報
     */
    @Override
    public AuthenticatedUser verify(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token) // tokenに関する例外のスローは、この部分で実施する。
                .getPayload();

        UserId userId = new UserId(Long.parseLong(claims.getSubject()));
        TenantId tenantId = new TenantId(claims.get("tenantId", Long.class));
        Role role = new Role(claims.get("role", String.class));

        return new AuthenticatedUser(userId, tenantId, role);
    }
}
