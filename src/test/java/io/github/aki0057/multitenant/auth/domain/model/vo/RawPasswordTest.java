package io.github.aki0057.multitenant.auth.domain.model.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link RawPassword} の単体テスト。
 */
public class RawPasswordTest {

    @Test
    @DisplayName("正常系: 有効なパスワードを渡すと value() が同じ値を返す。")
    void value_validValue() {
        RawPassword rawPassword = new RawPassword("password123");

        assertThat(rawPassword.value()).isEqualTo("password123");
    }

    @Test
    @DisplayName("正常系: 8 文字ちょうどを渡すと value() が同じ値を返す。")
    void value_minLength() {
        String value = "a".repeat(8);

        RawPassword rawPassword = new RawPassword(value);

        assertThat(rawPassword.value()).isEqualTo(value);
        assertThat(value).hasSize(8);
    }

    @Test
    @DisplayName("異常系: null を渡すと IllegalArgumentException がスローされる。")
    void constructor_null() {
        assertThatThrownBy(() -> new RawPassword(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 空文字を渡すと IllegalArgumentException がスローされる。")
    void constructor_empty() {
        assertThatThrownBy(() -> new RawPassword(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 空白のみを渡すと IllegalArgumentException がスローされる。")
    void constructor_blank() {
        assertThatThrownBy(() -> new RawPassword("        "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 8 文字未満を渡すと IllegalArgumentException がスローされる。")
    void constructor_tooShort() {
        assertThatThrownBy(() -> new RawPassword("abc123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 記号を含む値を渡すと IllegalArgumentException がスローされる。")
    void constructor_containsSymbol() {
        assertThatThrownBy(() -> new RawPassword("password!"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 全角文字を含む値を渡すと IllegalArgumentException がスローされる。")
    void constructor_containsFullWidthCharacter() {
        assertThatThrownBy(() -> new RawPassword("passワード123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 半角英数字と空白が混在した値を渡すと IllegalArgumentException がスローされる。")
    void constructor_containsWhitespaceMixedIn() {
        assertThatThrownBy(() -> new RawPassword("pass word123"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
