package io.github.aki0057.multitenant.auth.infrastructure.security;

import io.github.aki0057.multitenant.auth.domain.model.vo.RawRefreshToken;
import io.github.aki0057.multitenant.auth.domain.service.RefreshTokenGenerator;
import org.springframework.stereotype.Component;

/**
 * {@link RefreshTokenGenerator} の一時スタブアダプタ。
 * DI コンテナが具象 Bean を解決できるようにし、@SpringBootTest のコンテキスト起動を通すためだけに存在する。
 * セキュアランダムなトークン生成の実装は持たない。
 */
// TODO: セキュアランダム実装へ置換（後続 infrastructure 増分）
@Component
public class StubRefreshTokenGenerator implements RefreshTokenGenerator {

    /**
     * 未実装のスタブメソッド。
     *
     * @return なし（常に {@link UnsupportedOperationException} をスローする）
     */
    @Override
    public RawRefreshToken generate() {
        throw new UnsupportedOperationException("RefreshTokenGenerator#generate は未実装です");
    }
}
