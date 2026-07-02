package io.github.aki0057.multitenant.auth.domain.model.vo;

import java.util.regex.Pattern;

/**
 * テナントを識別するコード（DB の {@code tenants.tenant_code}）を表す Value Object。
 *
 * <p>DB の {@code VARCHAR(50)} カラムをラップし、infrastructure.mapper が
 * JPA エンティティをドメインモデルへ変換する際に使用される。</p>
 *
 * <p>テナントコードは必須であり、半角英数字（{@code a-z}, {@code A-Z}, {@code 0-9}）
 * のみを許容する。{@code null}・空欄・50 文字超・英数字以外を含む値は無効値とみなし、
 * 生成時に {@link IllegalArgumentException} をスローする。</p>
 *
 * @param value テナントコード（1〜50 文字の半角英数字）
 */
public record TenantCode(String value) {

    /** DB の VARCHAR(50) に合わせた最大長。 */
    private static final int MAX_LENGTH = 50;

    /** 半角英数字のみを許容するパターン。 */
    private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

    /**
     * テナントコードを検証する。
     *
     * @param value テナントコード
     * @throws IllegalArgumentException {@code value} が {@code null}・空欄・
     *         {@value #MAX_LENGTH} 文字超・英数字以外を含む場合
     */
    public TenantCode {
        if (value == null) {
            throw new IllegalArgumentException("TenantCode はnullにできません。");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("TenantCode は空欄にできません。");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "TenantCode は" + MAX_LENGTH + " 文字以内である必要があります。");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "テナントコードは半角英数字のみ使用できます。");
        }
    }
}
