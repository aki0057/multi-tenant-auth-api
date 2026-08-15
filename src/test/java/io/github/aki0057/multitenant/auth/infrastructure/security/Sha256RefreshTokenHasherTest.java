package io.github.aki0057.multitenant.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aki0057.multitenant.auth.domain.model.vo.RawRefreshToken;
import io.github.aki0057.multitenant.auth.domain.model.vo.TokenHash;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link Sha256RefreshTokenHasher} のユニットテスト。
 *
 * <p>技術アダプタ（ドメインポート実装）のため Spring コンテキストは起動せず、
 * コンストラクタで直接インスタンス化して検証する。</p>
 */
class Sha256RefreshTokenHasherTest {

    /**
     * 入力 "abc" に対する SHA-256 ハッシュの既知の期待値
     * （FIPS 180-2 の SHA-256 テストベクタ）。
     */
    private static final String SHA256_OF_ABC =
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    private final Sha256RefreshTokenHasher hasher = new Sha256RefreshTokenHasher();

    // ---------------------------------------------------------------
    // 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: 既知の入力に対して SHA-256 の期待値（64 桁 hex 文字列）と一致するハッシュが返る。")
    void hash_knownInputProducesExpectedSha256() {
        TokenHash result = hasher.hash(new RawRefreshToken("abc"));

        assertThat(result.value()).isEqualTo(SHA256_OF_ABC);
    }

    @Test
    @DisplayName("正常系: 同一の入力からは常に同一のハッシュが返る。")
    void hash_sameInputProducesSameHash() {
        RawRefreshToken input = new RawRefreshToken("same-input-token");

        TokenHash first = hasher.hash(input);
        TokenHash second = hasher.hash(input);

        assertThat(first.value()).isEqualTo(second.value());
    }
}
