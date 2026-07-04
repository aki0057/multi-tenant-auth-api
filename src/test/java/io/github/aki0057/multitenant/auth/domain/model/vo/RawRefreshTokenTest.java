package io.github.aki0057.multitenant.auth.domain.model.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link RawRefreshToken} の単体テスト。
 */
public class RawRefreshTokenTest {

    @Test
    @DisplayName("正常系: 空欄でない文字列を渡すと value() が同じ値を返す。")
    void value_validValue() {
        RawRefreshToken rawRefreshToken = new RawRefreshToken("raw-refresh-token-value");

        assertThat(rawRefreshToken.value()).isEqualTo("raw-refresh-token-value");
    }

    @Test
    @DisplayName("異常系: null を渡すと IllegalArgumentException がスローされる。")
    void constructor_null() {
        assertThatThrownBy(() -> new RawRefreshToken(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 空文字を渡すと IllegalArgumentException がスローされる。")
    void constructor_empty() {
        assertThatThrownBy(() -> new RawRefreshToken(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 空白のみを渡すと IllegalArgumentException がスローされる。")
    void constructor_blank() {
        assertThatThrownBy(() -> new RawRefreshToken("        "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
