package io.github.aki0057.multitenant.auth.infrastructure.security;

import io.github.aki0057.multitenant.auth.domain.service.RefreshTokenExpirationPolicy;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * {@link RefreshTokenExpirationPolicy} の一時スタブアダプタ。
 * DI コンテナが具象 Bean を解決できるようにし、@SpringBootTest のコンテキスト起動を通すためだけに存在する。
 * 有効期間の取得の実装は持たない。
 */
// TODO: JwtProperties 参照実装へ置換（後続 infrastructure 増分）
@Component
public class StubRefreshTokenExpirationPolicy implements RefreshTokenExpirationPolicy {

    /**
     * 未実装のスタブメソッド。
     *
     * @return なし（常に {@link UnsupportedOperationException} をスローする）
     */
    @Override
    public Duration expiration() {
        throw new UnsupportedOperationException("RefreshTokenExpirationPolicy#expiration は未実装です");
    }
}
