package io.github.aki0057.multitenant.auth.infrastructure.security;

import io.github.aki0057.multitenant.auth.domain.model.vo.RawRefreshToken;
import io.github.aki0057.multitenant.auth.domain.model.vo.TokenHash;
import io.github.aki0057.multitenant.auth.domain.service.RefreshTokenHasher;
import org.springframework.stereotype.Component;

/**
 * {@link RefreshTokenHasher} の一時スタブアダプタ。
 * DI コンテナが具象 Bean を解決できるようにし、@SpringBootTest のコンテキスト起動を通すためだけに存在する。
 * SHA-256 によるハッシュ計算の実装は持たない。
 */
// TODO: SHA-256 実装へ置換（後続 infrastructure 増分）
@Component
public class StubRefreshTokenHasher implements RefreshTokenHasher {

    /**
     * 未実装のスタブメソッド。
     *
     * @param rawRefreshToken 生のリフレッシュトークン
     * @return なし（常に {@link UnsupportedOperationException} をスローする）
     */
    @Override
    public TokenHash hash(RawRefreshToken rawRefreshToken) {
        throw new UnsupportedOperationException("RefreshTokenHasher#hash は未実装です");
    }
}
