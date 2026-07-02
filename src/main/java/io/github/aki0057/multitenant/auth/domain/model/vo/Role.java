package io.github.aki0057.multitenant.auth.domain.model.vo;

import java.util.Set;

/**
 * ユーザーロール（DB の {@code users.role}）を表す Value Object。
 *
 * <p>DB の {@code VARCHAR(20)} カラムをラップし、infrastructure.mapper が
 * JPA エンティティをドメインモデルへ変換する際、および JWT の {@code role} クレームから
 * 復元する際に使用される。</p>
 *
 * <p>ロールは必須であり、{@code "ADMIN"} または {@code "USER"} のいずれかのみを許容する
 * （{@code docs/database-design.md} の {@code users.role} 備考欄に準拠）。
 * {@code null}・空欄・20 文字超・許容値以外の値は無効値とみなし、
 * 生成時に {@link IllegalArgumentException} をスローする。</p>
 *
 * @param value ロール（{@code "ADMIN"} または {@code "USER"}）
 */
public record Role(String value) {

    /** DB の VARCHAR(20) に合わせた最大長。 */
    private static final int MAX_LENGTH = 20;

    /** 許容するロールのホワイトリスト。 */
    private static final Set<String> ALLOWED_VALUES = Set.of("ADMIN", "USER");

    /**
     * ロールを検証する。
     *
     * @param value ロール
     * @throws IllegalArgumentException {@code value} が {@code null}・空欄・
     *         {@value #MAX_LENGTH} 文字超・許容値（{@code "ADMIN"} / {@code "USER"}）以外の場合
     */
    public Role {
        if (value == null) {
            throw new IllegalArgumentException("Role はnullにできません。");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Role は空欄にできません。");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Role は" + MAX_LENGTH + " 文字以内である必要があります。");
        }
        if (!ALLOWED_VALUES.contains(value)) {
            throw new IllegalArgumentException(
                    "Role は " + ALLOWED_VALUES + " のいずれかである必要があります。");
        }
    }
}

