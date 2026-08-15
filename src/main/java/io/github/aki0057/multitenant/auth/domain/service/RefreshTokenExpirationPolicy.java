package io.github.aki0057.multitenant.auth.domain.service;

import java.time.Duration;

/**
 * リフレッシュトークンの有効期間を提供するドメインポート。
 * 有効期間の値の取得元（設定プロパティ等）の詳細は infrastructure 層の実装に委ねる。
 */
public interface RefreshTokenExpirationPolicy {

    /**
     * リフレッシュトークンの有効期間を返す。
     *
     * @return リフレッシュトークンの有効期間
     */
    Duration expiration();
}
