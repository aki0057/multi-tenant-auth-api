package io.github.aki0057.multitenant.auth.domain.exception;

/**
 * リフレッシュトークンが無効であることを表すドメイン例外。
 * 不存在・期限切れ・失効済み・ユーザー無効・テナント無効のいずれであっても、
 * 攻撃者へ手掛かりを与えないよう理由を区別せず本例外で表現する。
 * Spring などの外部フレームワークには依存しない。
 */
public class InvalidRefreshTokenException extends RuntimeException {

    /**
     * 無効リフレッシュトークン例外を生成する。
     */
    public InvalidRefreshTokenException() {
        super("Invalid refresh token");
    }
}
