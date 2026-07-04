package io.github.aki0057.multitenant.auth.domain.model.vo;

/**
 * クライアントへ渡す生のリフレッシュトークン文字列を表す Value Object。
 *
 * <p>サーバー側で永続化されるのはハッシュ値（{@link TokenHash}）のみであり、
 * 生トークンはレスポンスとして返却する時のみ扱う。</p>
 *
 * <p>生のリフレッシュトークンは必須である。{@code null}・空白のみの値は無効値とみなし、
 * 生成時に {@link IllegalArgumentException} をスローする。
 * 生成方式（{@code domain.service.RefreshTokenGenerator} の infrastructure 実装）が
 * 未確定のため、形式チェック（長さ・文字種）は行わず緩い検証に留める。</p>
 *
 * @param value 生のリフレッシュトークン文字列
 */
public record RawRefreshToken(String value) {

    /**
     * 生のリフレッシュトークン文字列を検証する。
     *
     * @param value 生のリフレッシュトークン文字列
     * @throws IllegalArgumentException {@code value} が {@code null}・空白のみの場合
     */
    public RawRefreshToken {
        if (value == null) {
            throw new IllegalArgumentException("RawRefreshToken はnullにできません。");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("RawRefreshToken は空欄にできません。");
        }
    }
}
