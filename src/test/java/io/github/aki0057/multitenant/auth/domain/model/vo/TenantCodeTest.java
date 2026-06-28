package io.github.aki0057.multitenant.auth.domain.model.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link TenantCode} の単体テスト。
 */
public class TenantCodeTest {

    @Test
    @DisplayName("正常系: 半角英数字を渡すと value() が同じ値を返す。")
    void value_validValue() {
        TenantCode tenantCode = new TenantCode("testTenant");

        assertThat(tenantCode.value()).isEqualTo("testTenant");
    }

    @Test
    @DisplayName("正常系: 50 文字ちょうどを渡すと value() が同じ値を返す。")
    void value_maxLength() {
        String value = "a".repeat(50);

        TenantCode tenantCode = new TenantCode(value);

        assertThat(tenantCode.value()).isEqualTo(value);
    }

    @Test
    @DisplayName("異常系: null を渡すと IllegalArgumentException がスローされる。")
    void constructor_null() {
        assertThatThrownBy(() -> new TenantCode(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 空文字を渡すと IllegalArgumentException がスローされる。")
    void constructor_empty() {
        assertThatThrownBy(() -> new TenantCode(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 空白のみを渡すと IllegalArgumentException がスローされる。")
    void constructor_blank() {
        assertThatThrownBy(() -> new TenantCode(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 50 文字超を渡すと IllegalArgumentException がスローされる。")
    void constructor_tooLong() {
        String value = "a".repeat(51);

        assertThatThrownBy(() -> new TenantCode(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 記号を含む値を渡すと IllegalArgumentException がスローされる。")
    void constructor_symbol() {
        assertThatThrownBy(() -> new TenantCode("test-tenant"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 全角文字を含む値を渡すと IllegalArgumentException がスローされる。")
    void constructor_fullWidth() {
        assertThatThrownBy(() -> new TenantCode("テストテナント"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}