package io.github.aki0057.multitenant.auth.domain.model.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link PasswordHash} の単体テスト。
 */
public class PasswordHashTest {

    private static final String VALID_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Test
    @DisplayName("正常系: 有効な BCrypt 形式を渡すと value() が同じ値を返す。")
    void value_validValue() {
        PasswordHash passwordHash = new PasswordHash(VALID_HASH);

        assertThat(passwordHash.value()).isEqualTo(VALID_HASH);
    }

    @Test
    @DisplayName("異常系: null を渡すと IllegalArgumentException がスローされる。")
    void constructor_null() {
        assertThatThrownBy(() -> new PasswordHash(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 空文字を渡すと IllegalArgumentException がスローされる。")
    void constructor_empty() {
        assertThatThrownBy(() -> new PasswordHash(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 空白のみを渡すと IllegalArgumentException がスローされる。")
    void constructor_blank() {
        assertThatThrownBy(() -> new PasswordHash(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 255 文字超を渡すと IllegalArgumentException がスローされる。")
    void constructor_tooLong() {
        String value = "a".repeat(256);

        assertThatThrownBy(() -> new PasswordHash(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 接頭辞が不正な値を渡すと IllegalArgumentException がスローされる。")
    void constructor_invalidPrefix() {
        String value = "$2x$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

        assertThatThrownBy(() -> new PasswordHash(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: コストパラメータの桁数が不正な値を渡すと IllegalArgumentException がスローされる。")
    void constructor_invalidCostDigits() {
        String value = "$2a$1$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

        assertThatThrownBy(() -> new PasswordHash(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: salt+hash 部の長さが不正な値を渡すと IllegalArgumentException がスローされる。")
    void constructor_invalidSaltHashLength() {
        String value = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhW";

        assertThatThrownBy(() -> new PasswordHash(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: salt+hash 部に許容外の記号を含む値を渡すと IllegalArgumentException がスローされる。")
    void constructor_containsSymbol() {
        String value = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh!y";

        assertThatThrownBy(() -> new PasswordHash(value))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
