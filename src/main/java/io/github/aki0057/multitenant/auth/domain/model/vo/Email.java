package io.github.aki0057.multitenant.auth.domain.model.vo;

import java.util.regex.Pattern;

/**
 * メールアドレスを表す Value Object（DB の {@code users.email}）。
 *
 * <p>DB の {@code VARCHAR(254)} カラムをラップし、infrastructure.mapper が
 * JPA エンティティをドメインモデルへ変換する際に使用される。</p>
 *
 * <p>メールアドレスは必須であり、一般的なメールアドレス形式
 * （{@code ローカル部@ドメイン部}、{@code @} を 1 つのみ含み、ドメイン部にドット区切りの
 * ラベルを持つ）に一致する必要がある。{@code null}・空欄・254 文字超・形式不一致の値は
 * 無効値とみなし、生成時に {@link IllegalArgumentException} をスローする。</p>
 *
 * @param value メールアドレス（1〜254 文字のメールアドレス形式）
 */
public record Email(String value) {

    /** DB の VARCHAR(254) に合わせた最大長。 */
    private static final int MAX_LENGTH = 254;

    /** 一般的なメールアドレス形式のみを許容するパターン。 */
    private static final Pattern PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /**
     * メールアドレスを検証する。
     *
     * @param value メールアドレス
     * @throws IllegalArgumentException {@code value} が {@code null}・空欄・
     *         {@value #MAX_LENGTH} 文字超・メールアドレス形式に一致しない場合
     */
    public Email {
        if (value == null) {
            throw new IllegalArgumentException("Email はnullにできません。");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Email は空欄にできません。");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Email は" + MAX_LENGTH + " 文字以内である必要があります。");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Email はメールアドレス形式である必要があります。");
        }
    }
}

