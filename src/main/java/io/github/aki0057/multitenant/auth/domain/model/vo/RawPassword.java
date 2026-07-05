package io.github.aki0057.multitenant.auth.domain.model.vo;

import org.jspecify.annotations.NonNull;

import java.util.regex.Pattern;

/**
 * 平文パスワードを表す Value Object。
 *
 * <p>ログイン時にユーザーが入力する平文パスワードをラップし、
 * {@code domain.service.PasswordVerifier} や
 * {@code infrastructure.security.PasswordEncoderVerifier} がハッシュ値との照合に使用する。</p>
 *
 * <p>パスワードは必須であり、8 文字以上かつ半角英数字（{@code A-Za-z0-9}）のみで
 * 構成される必要がある。{@code null}・空白のみ・8 文字未満・半角英数字以外を含む値は
 * 無効値とみなし、生成時に {@link IllegalArgumentException} をスローする。</p>
 *
 * @param value 平文パスワード（8 文字以上の半角英数字）
 */
public record RawPassword(String value) {

    /** 許容する最小文字数。 */
    private static final int MIN_LENGTH = 8;

    /** 半角英数字のみを許容するパターン。 */
    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9]+$");

    /**
     * 平文パスワードを検証する。
     *
     * @param value 平文パスワード
     * @throws IllegalArgumentException {@code value} が {@code null}・空白のみ・
     *         {@value #MIN_LENGTH} 文字未満・半角英数字以外を含む場合
     */
    public RawPassword {
        if (value == null) {
            throw new IllegalArgumentException("RawPassword はnullにできません。");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("RawPassword は空欄にできません。");
        }
        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "RawPassword は" + MIN_LENGTH + " 文字以上である必要があります。");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "RawPassword は半角英数字のみで構成される必要があります。");
        }
    }

    /**
     * 値を含まないマスキング済み固定文字列を返す。
     *
     * <p>record の自動生成 {@code toString()} は平文パスワードをそのまま含むため、
     * ログ出力・例外メッセージ・デバッガ表示などを通じて機密値が流出するリスクがある。
     * これを防ぐためにオーバーライドし、常に値を含まない固定文字列を返す。</p>
     *
     * @return マスキング済み固定文字列 {@code "RawPassword[masked]"}
     */
    @Override
    @NonNull
    public String toString() {
        return "RawPassword[masked]";
    }
}

