package io.github.aki0057.multitenant.auth.domain.model.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link Role} の単体テスト。
 */
public class RoleTest {

    @Test
    @DisplayName("正常系: ADMIN を渡すと value() が同じ値を返す。")
    void value_admin() {
        Role role = new Role("ADMIN");

        assertThat(role.value()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("正常系: USER を渡すと value() が同じ値を返す。")
    void value_user() {
        Role role = new Role("USER");

        assertThat(role.value()).isEqualTo("USER");
    }

    @Test
    @DisplayName("異常系: null を渡すと IllegalArgumentException がスローされる。")
    void constructor_null() {
        assertThatThrownBy(() -> new Role(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 空文字を渡すと IllegalArgumentException がスローされる。")
    void constructor_empty() {
        assertThatThrownBy(() -> new Role(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 空白のみを渡すと IllegalArgumentException がスローされる。")
    void constructor_blank() {
        assertThatThrownBy(() -> new Role(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 許容外の値を渡すと IllegalArgumentException がスローされる。")
    void constructor_notAllowedValue() {
        assertThatThrownBy(() -> new Role("SUPER_ADMIN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 20 文字超を渡すと IllegalArgumentException がスローされる。")
    void constructor_tooLong() {
        String value = "a".repeat(21);

        assertThatThrownBy(() -> new Role(value))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
