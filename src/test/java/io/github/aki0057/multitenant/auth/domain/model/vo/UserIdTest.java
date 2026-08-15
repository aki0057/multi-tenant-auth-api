package io.github.aki0057.multitenant.auth.domain.model.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

    /**
     * {@link UserId} の単体テスト。
     */
    public class UserIdTest {

        @Test
        @DisplayName("正常系: 1 以上の正整数を渡すと value() が同じ値を返す。")
        void value_validValue() {
            UserId userId = new UserId(1L);

            assertThat(userId.value()).isEqualTo(1L);
        }

        @Test
        @DisplayName("異常系: null を渡すと IllegalArgumentException がスローされる。")
        void constructor_null() {
            assertThatThrownBy(() -> new UserId(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("異常系: 0 を渡すと IllegalArgumentException がスローされる。")
        void constructor_zero() {
            assertThatThrownBy(() -> new UserId(0L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("異常系: 負の値を渡すと IllegalArgumentException がスローされる。")
        void constructor_negative() {
            assertThatThrownBy(() -> new UserId(-1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

