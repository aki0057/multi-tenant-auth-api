package io.github.aki0057.multitenant.auth.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT アクセストークン発行に用いる設定値を保持する Configuration Properties。
 *
 * <p>{@code application.properties} の {@code jwt} プレフィックスにバインドされる。
 * record のため自動的にコンストラクタバインドが適用される（{@code @ConstructorBinding} は不要）。</p>
 *
 * <p>Bean 登録は {@link JwtConfig} の {@code @EnableConfigurationProperties} により行われる。</p>
 *
 * @param secret            HS256 署名に用いる秘密鍵文字列（{@code jwt.secret} にバインド）
 * @param expiration        アクセストークンの有効期間（{@code jwt.expiration} にバインド）
 * @param refreshExpiration リフレッシュトークンの有効期間（{@code jwt.refresh-expiration} にバインド）
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        Duration expiration,
        Duration refreshExpiration
) {
}
