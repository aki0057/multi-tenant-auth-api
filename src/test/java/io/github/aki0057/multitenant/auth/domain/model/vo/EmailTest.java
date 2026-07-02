package io.github.aki0057.multitenant.auth.domain.model.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link Email} の単体テスト。
 */
public class EmailTest {

    @Test
    @DisplayName("正常系: 有効なメールアドレスを渡すと value() が同じ値を返す。")
    void value_validValue() {
        Email email = new Email("test@example.com");

        assertThat(email.value()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("正常系: 254 文字ちょうどを渡すと value() が同じ値を返す。")
    void value_maxLength() {
        String value = "a".repeat(242) + "@example.com";

        Email email = new Email(value);

        assertThat(email.value()).isEqualTo(value);
        assertThat(value).hasSize(254);
    }

    @Test
    @DisplayName("異常系: null を渡すと IllegalArgumentException がスローされる。")
    void constructor_null() {
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 空文字を渡すと IllegalArgumentException がスローされる。")
    void constructor_empty() {
        assertThatThrownBy(() -> new Email(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 空白のみを渡すと IllegalArgumentException がスローされる。")
    void constructor_blank() {
        assertThatThrownBy(() -> new Email(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: @ を含まない値を渡すと IllegalArgumentException がスローされる。")
    void constructor_noAtMark() {
        assertThatThrownBy(() -> new Email("test.example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: @ を複数含む値を渡すと IllegalArgumentException がスローされる。")
    void constructor_multipleAtMarks() {
        assertThatThrownBy(() -> new Email("test@ex@ample.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: ドメイン部が欠如した値を渡すと IllegalArgumentException がスローされる。")
    void constructor_missingDomain() {
        assertThatThrownBy(() -> new Email("test@"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("異常系: 254 文字超を渡すと IllegalArgumentException がスローされる。")
    void constructor_tooLong() {
        String value = "a".repeat(243) + "@example.com";

        assertThatThrownBy(() -> new Email(value))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
