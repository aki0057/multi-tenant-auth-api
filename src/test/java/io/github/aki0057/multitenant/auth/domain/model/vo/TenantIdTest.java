package io.github.aki0057.multitenant.auth.domain.model.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link TenantId} の単体テスト。
 */
class TenantIdTest {

    @Test
    @DisplayName("正常系: 1 以上の正整数を渡すと value() が同じ値を返す。")
    void value_validValue() {
        TenantId tenantId = new TenantId(1L);

        assertThat(tenantId.value()).isEqualTo(1L);
    }

    @Test
    @DisplayName("異常系: null を渡すと IllegalArgumentException がスローされる。")
    void constructor_null() {
        assertThatThrownBy(() -> new TenantId(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 0 を渡すと IllegalArgumentException がスローされる。")
    void constructor_zero() {
        assertThatThrownBy(() -> new TenantId(0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 負の値を渡すと IllegalArgumentException がスローされる。")
    void constructor_negative() {
        assertThatThrownBy(() -> new TenantId(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
