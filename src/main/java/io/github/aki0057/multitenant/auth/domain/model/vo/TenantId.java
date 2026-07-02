package io.github.aki0057.multitenant.auth.domain.model.vo;

/**
 * テナントの主キー（DB の {@code tenants.id}）を表す Value Object。
 *
 * <p>DB の {@code BIGSERIAL} 主キーをラップし、infrastructure.mapper が
 * JPA エンティティをドメインモデルへ変換する際に使用される。</p>
 *
 * <p>主キーは 1 以上の正整数であるため、{@code null} および 0 以下の値は
 * 無効値とみなし、生成時に {@link IllegalArgumentException} をスローする。</p>
 *
 * @param value テナントの主キー値（1以上の整数）
 */
public record TenantId(Long value) {

    /**
     * テナントの主キー値を検証する。
     *
     * @param value テナントの主キー値
     * @throws IllegalArgumentException {@code value} が {@code null} または 0 以下の場合
     */
    public TenantId {
        if (value == null) {
            throw new IllegalArgumentException("TenantId はnullにできません。");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("TenantId は1以上の整数である必要があります。");
        }
    }
}

