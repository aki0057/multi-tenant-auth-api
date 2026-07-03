package io.github.aki0057.multitenant.auth.domain.model.vo;

/**
 * リフレッシュトークンの SHA-256 ハッシュ値（hex 文字列）を表す Value Object。
 * DB にはこのハッシュ値のみを保存し、生トークンは永続化しない。
 *
 * <p>スタブ: 値検証ロジックは未実装（後続 domain 増分で実装する）。</p>
 *
 * @param value SHA-256 ハッシュ値の hex 文字列
 */
// TODO: 値検証ロジックを実装（後続 domain 増分）
public record TokenHash(String value) {}
