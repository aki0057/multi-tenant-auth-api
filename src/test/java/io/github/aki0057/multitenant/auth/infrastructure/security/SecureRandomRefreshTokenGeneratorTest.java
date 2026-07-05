package io.github.aki0057.multitenant.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aki0057.multitenant.auth.domain.model.vo.RawRefreshToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link SecureRandomRefreshTokenGenerator} のユニットテスト。
 *
 * <p>技術アダプタ（ドメインポート実装）のため Spring コンテキストは起動せず、
 * コンストラクタで直接インスタンス化して検証する。</p>
 */
class SecureRandomRefreshTokenGeneratorTest {

    private final SecureRandomRefreshTokenGenerator generator =
            new SecureRandomRefreshTokenGenerator();

    // ---------------------------------------------------------------
    // 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: 生成された生リフレッシュトークンは空でない値を持つ。")
    void generate_returnsNonBlankValue() {
        RawRefreshToken token = generator.generate();

        assertThat(token.value()).isNotBlank();
    }

    @Test
    @DisplayName("正常系: 呼び出しごとに異なる値が生成される。")
    void generate_returnsDifferentValuesEachCall() {
        RawRefreshToken first = generator.generate();
        RawRefreshToken second = generator.generate();

        assertThat(first.value()).isNotEqualTo(second.value());
    }
}
