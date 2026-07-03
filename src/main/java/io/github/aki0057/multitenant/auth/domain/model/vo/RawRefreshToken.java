package io.github.aki0057.multitenant.auth.domain.model.vo;

/**
 * クライアントへ渡す生のリフレッシュトークン文字列を表す Value Object。
 * サーバー側で永続化されるのはハッシュ値（{@link TokenHash}）のみであり、
 * 生トークンはレスポンスとして返却する時のみ扱う。
 *
 * <p>スタブ: 値検証ロジックは未実装（後続 domain 増分で実装する）。</p>
 *
 * @param value 生のリフレッシュトークン文字列
 */
// TODO: 値検証ロジックを実装（後続 domain 増分）
public record RawRefreshToken(String value) {}
