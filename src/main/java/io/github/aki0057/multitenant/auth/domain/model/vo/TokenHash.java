package io.github.aki0057.multitenant.auth.domain.model.vo;

import java.util.regex.Pattern;

/**
 * リフレッシュトークンの SHA-256 ハッシュ値（hex 文字列）を表す Value Object
 * （DB の {@code refresh_tokens.token_hash}）。
 * DB にはこのハッシュ値のみを保存し、生トークンは永続化しない。
 *
 * <p>DB の {@code VARCHAR(255)} カラムをラップし、infrastructure.mapper が
 * JPA エンティティをドメインモデルへ変換する際に使用される。</p>
 *
 * <p>ハッシュ値は必須であり、SHA-256 の hex 文字列形式（0-9 / a-f / A-F のみで
 * 構成される 64 桁。SHA-256 のダイジェスト長は 32 バイト = 64 桁の hex 文字列）に
 * 一致する必要がある。{@code null}・空欄・形式不一致の値は無効値とみなし、
 * 生成時に {@link IllegalArgumentException} をスローする。</p>
 *
 * @param value SHA-256 ハッシュ値の hex 文字列（64 桁の 16 進数）
 */
public record TokenHash(String value) {

    /** SHA-256 の hex 文字列形式（64 桁の 16 進数）のみを許容するパターン。 */
    private static final Pattern PATTERN =
            Pattern.compile("^[0-9a-fA-F]{64}$");

    /**
     * SHA-256 ハッシュ値の hex 文字列を検証する。
     *
     * @param value SHA-256 ハッシュ値の hex 文字列
     * @throws IllegalArgumentException {@code value} が {@code null}・空欄・
     *         SHA-256 の hex 文字列形式に一致しない場合
     */
    public TokenHash {
        if (value == null) {
            throw new IllegalArgumentException("TokenHash はnullにできません。");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("TokenHash は空欄にできません。");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "TokenHash はSHA-256のhex文字列（64桁の16進数）である必要があります。");
        }
    }
}
