package io.github.aki0057.multitenant.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

/**
 * {@link JwtRefreshTokenExpirationPolicy} のユニットテスト。
 *
 * <p>技術アダプタ（ドメインポート実装）のため Spring コンテキストは起動せず、
 * {@link JwtProperties} を直接インスタンス化してコンストラクタで生成する。</p>
 */
class JwtRefreshTokenExpirationPolicyTest {

    // ---------------------------------------------------------------
    // 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: JwtProperties の refreshExpiration の値がそのまま返る。")
    void expiration_returnsValueFromJwtProperties() {
        JwtProperties properties = new JwtProperties(
                "test-secret-key-for-hs256-which-is-long-enough",
                Duration.ofMinutes(15),
                Duration.ofDays(14));
        JwtRefreshTokenExpirationPolicy policy = new JwtRefreshTokenExpirationPolicy(properties);

        Duration result = policy.expiration();

        assertThat(result).isEqualTo(Duration.ofDays(14));
    }
}
