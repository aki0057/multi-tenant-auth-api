package io.github.aki0057.multitenant.auth.infrastructure.security;

import io.github.aki0057.multitenant.auth.domain.model.vo.RawRefreshToken;
import io.github.aki0057.multitenant.auth.domain.model.vo.TokenHash;
import io.github.aki0057.multitenant.auth.domain.service.RefreshTokenHasher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * {@link RefreshTokenHasher} の実装アダプタ。
 * 生リフレッシュトークンを SHA-256 でハッシュ化し、
 * {@link TokenHash} の期待形式（64 桁の hex 文字列）で返す。
 */
@Component
public class Sha256RefreshTokenHasher implements RefreshTokenHasher {

    /**
     * 生のリフレッシュトークンを SHA-256 でハッシュ化する。
     *
     * @param rawRefreshToken 生のリフレッシュトークン
     * @return ハッシュ値（64 桁の hex 文字列）
     * @throws IllegalStateException SHA-256 アルゴリズムが利用できない場合
     *         （Java 実行環境では必ず提供されるため、通常は発生しない）
     */
    @Override
    public TokenHash hash(RawRefreshToken rawRefreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(
                    rawRefreshToken.value().getBytes(StandardCharsets.UTF_8));
            return new TokenHash(HexFormat.of().formatHex(hashed));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 アルゴリズムが利用できません。", e);
        }
    }
}
