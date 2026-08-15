package io.github.aki0057.multitenant.auth.domain.model.vo;

/**
 * ユーザーの主キー（DB の {@code users.id}）を表す Value Object。
 *
 * <p>DB の {@code BIGSERIAL} 主キーをラップし、infrastructure.mapper が
 * JPA エンティティをドメインモデルへ変換する際に使用される。</p>
 *
 * <p>主キーは 1 以上の正整数であるため、{@code null} および 0 以下の値は
 * 無効値とみなし、生成時に {@link IllegalArgumentException} をスローする。</p>
 *
 * @param value テナントの主キー値（1以上の整数）
 */
public record UserId(Long value) {

    /**
     * テナントの主キー値を検証する。
     *
     * @param value ユーザーの主キー値
     * @throws IllegalArgumentException {@code value} が {@code null} または 0 以下の場合
     */
    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("UserId はnullにできません。");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("UserId は1以上の整数である必要があります。");
        }
    }
}

