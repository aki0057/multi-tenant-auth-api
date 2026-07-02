package io.github.aki0057.multitenant.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aki0057.multitenant.auth.domain.model.vo.PasswordHash;
import io.github.aki0057.multitenant.auth.domain.model.vo.RawPassword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * {@link PasswordEncoderVerifier} のユニットテスト。
 *
 * <p>技術アダプタ（ドメインポート実装）のため Spring コンテキストは起動せず、
 * 本番と同一実装である {@link BCryptPasswordEncoder} を直接インスタンス化し、
 * {@link PasswordEncoderVerifier} をコンストラクタで生成して照合を検証する。</p>
 */
class PasswordEncoderVerifierTest {

    /** 照合対象の生パスワード（8 文字以上の半角英数字）。 */
    private static final String RAW = "password123";

    private BCryptPasswordEncoder passwordEncoder;
    private PasswordEncoderVerifier verifier;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        verifier = new PasswordEncoderVerifier(passwordEncoder);
    }

    // ---------------------------------------------------------------
    // 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: 生パスワードと一致する BCrypt ハッシュを照合すると true を返す。")
    void matches_rawPasswordMatchesHash() {
        PasswordHash hash = new PasswordHash(passwordEncoder.encode(RAW));

        boolean result = verifier.matches(new RawPassword(RAW), hash);

        assertThat(result).isTrue();
    }

    // ---------------------------------------------------------------
    // 異常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("異常系: 生パスワードと一致しない BCrypt ハッシュを照合すると false を返す。")
    void matches_rawPasswordDoesNotMatchHash() {
        PasswordHash hash = new PasswordHash(passwordEncoder.encode("different456"));

        boolean result = verifier.matches(new RawPassword(RAW), hash);

        assertThat(result).isFalse();
    }
}
