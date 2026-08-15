package io.github.aki0057.multitenant.auth.infrastructure.security;

import io.github.aki0057.multitenant.auth.domain.model.vo.RawRefreshToken;
import io.github.aki0057.multitenant.auth.domain.service.RefreshTokenGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * {@link RefreshTokenGenerator} の実装アダプタ。
 * {@link SecureRandom} で 256bit（32 バイト）のエントロピーを持つランダムバイト列を生成し、
 * URL-safe Base64（パディングなし）で文字列化した生リフレッシュトークンを返す。
 */
@Component
public class SecureRandomRefreshTokenGenerator implements RefreshTokenGenerator {

    /** 生成するランダムバイト列の長さ（256bit = 32 バイト）。 */
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * セキュアランダムな生リフレッシュトークンを生成する。
     *
     * @return 生成された生リフレッシュトークン（URL-safe Base64・パディングなしの文字列）
     */
    @Override
    public RawRefreshToken generate() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return new RawRefreshToken(
                Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }
}
