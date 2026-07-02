package io.github.aki0057.multitenant.auth.domain.model.vo;

import java.util.regex.Pattern;

/**
 * ハッシュ化済みパスワードを表す Value Object（DB の {@code users.password_hash}）。
 *
 * <p>DB の {@code VARCHAR(255)} カラムをラップし、infrastructure.mapper が
 * JPA エンティティをドメインモデルへ変換する際に使用される。</p>
 *
 * <p>ハッシュ値は必須であり、BCrypt 形式（接頭辞 {@code $2a$} / {@code $2b$} / {@code $2y$}、
 * コストパラメータ 2 桁、salt+hash 部 53 文字の計 60 文字）に一致する必要がある。
 * {@code null}・空欄・255 文字超・BCrypt 形式に一致しない値は無効値とみなし、
 * 生成時に {@link IllegalArgumentException} をスローする。</p>
 *
 * @param value ハッシュ化済みパスワード（BCrypt 形式）
 */
public record PasswordHash(String value) {

    /** DB の VARCHAR(255) に合わせた最大長。 */
    private static final int MAX_LENGTH = 255;

    /** BCrypt 形式のみを許容するパターン。 */
    private static final Pattern PATTERN =
            Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    /**
     * ハッシュ化済みパスワードを検証する。
     *
     * @param value ハッシュ化済みパスワード
     * @throws IllegalArgumentException {@code value} が {@code null}・空欄・
     *         {@value #MAX_LENGTH} 文字超・BCrypt 形式に一致しない場合
     */
    public PasswordHash {
        if (value == null) {
            throw new IllegalArgumentException("PasswordHash はnullにできません。");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("PasswordHash は空欄にできません。");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "PasswordHash は" + MAX_LENGTH + " 文字以内である必要があります。");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "PasswordHash はBCrypt形式である必要があります。");
        }
    }
}

