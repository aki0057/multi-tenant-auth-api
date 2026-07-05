package io.github.aki0057.multitenant.auth.infrastructure.security;

import io.github.aki0057.multitenant.auth.domain.service.RefreshTokenExpirationPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * {@link RefreshTokenExpirationPolicy} の実装アダプタ。
 * 設定プロパティ（{@code jwt.refresh-expiration}）にバインドされた
 * {@link JwtProperties} からリフレッシュトークンの有効期間を取得して返す。
 */
@Component
@RequiredArgsConstructor
public class JwtRefreshTokenExpirationPolicy implements RefreshTokenExpirationPolicy {

    private final JwtProperties jwtProperties;

    /**
     * リフレッシュトークンの有効期間を返す。
     *
     * @return リフレッシュトークンの有効期間（{@code jwt.refresh-expiration} の設定値）
     */
    @Override
    public Duration expiration() {
        return jwtProperties.refreshExpiration();
    }
}
