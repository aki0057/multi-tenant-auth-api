package io.github.aki0057.multitenant.auth.infrastructure.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 関連の設定クラス。
 *
 * <p>{@link JwtProperties} を {@code @EnableConfigurationProperties} で Bean 登録し、
 * {@link JwtAccessTokenProvider} へインジェクション可能にする。</p>
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {
}
