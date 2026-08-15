package io.github.aki0057.multitenant.auth.domain.exception;

/**
 * 認証に失敗したことを表すドメイン例外。
 * アカウント無効・テナント無効・パスワード不一致のいずれであっても、
 * 攻撃者へ手掛かりを与えないよう理由を区別せず本例外で表現する。
 * Spring などの外部フレームワークには依存しない。
 */
public class AuthenticationFailedException extends RuntimeException {

    /**
     * 認証失敗例外を生成する。
     */
    public AuthenticationFailedException() {
        super("Authentication failed");
    }
}
