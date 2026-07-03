package io.github.aki0057.multitenant.auth.domain.model.vo;

/**
 * リフレッシュトークンの主キー（DB の {@code refresh_tokens.id}）を表す Value Object。
 *
 * <p>スタブ: 値検証ロジックは未実装（後続 domain 増分で実装する）。</p>
 *
 * @param value リフレッシュトークンの主キー値
 */
// TODO: 値検証ロジックを実装（後続 domain 増分）
public record RefreshTokenId(Long value) {}
