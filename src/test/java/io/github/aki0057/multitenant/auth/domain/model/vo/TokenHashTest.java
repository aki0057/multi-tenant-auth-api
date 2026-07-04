package io.github.aki0057.multitenant.auth.domain.model.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link TokenHash} の単体テスト。
 */
public class TokenHashTest {

    private static final String VALID_HASH =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @Test
    @DisplayName("正常系: 有効な 64 桁 hex 文字列を渡すと value() が同じ値を返す。")
    void value_validValue() {
        TokenHash tokenHash = new TokenHash(VALID_HASH);

        assertThat(tokenHash.value()).isEqualTo(VALID_HASH);
    }

    @Test
    @DisplayName("正常系: 大文字を含む 64 桁 hex 文字列を渡すと value() が同じ値を返す。")
    void value_upperCaseHex() {
        String value = "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855";

        TokenHash tokenHash = new TokenHash(value);

        assertThat(tokenHash.value()).isEqualTo(value);
    }

    @Test
    @DisplayName("異常系: null を渡すと IllegalArgumentException がスローされる。")
    void constructor_null() {
        assertThatThrownBy(() -> new TokenHash(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 空文字を渡すと IllegalArgumentException がスローされる。")
    void constructor_empty() {
        assertThatThrownBy(() -> new TokenHash(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 空白のみを渡すと IllegalArgumentException がスローされる。")
    void constructor_blank() {
        assertThatThrownBy(() -> new TokenHash(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 64 桁未満の hex 文字列を渡すと IllegalArgumentException がスローされる。")
    void constructor_tooShort() {
        String value = "a".repeat(63);

        assertThatThrownBy(() -> new TokenHash(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 64 桁超過の hex 文字列を渡すと IllegalArgumentException がスローされる。")
    void constructor_tooLong() {
        String value = "a".repeat(65);

        assertThatThrownBy(() -> new TokenHash(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: hex 以外の文字（g）を含む値を渡すと IllegalArgumentException がスローされる。")
    void constructor_containsNonHexLetter() {
        String value = "g" + "a".repeat(63);

        assertThatThrownBy(() -> new TokenHash(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: hex 以外の文字（記号）を含む値を渡すと IllegalArgumentException がスローされる。")
    void constructor_containsSymbol() {
        String value = "a".repeat(63) + "-";

        assertThatThrownBy(() -> new TokenHash(value))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
