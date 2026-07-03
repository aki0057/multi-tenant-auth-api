package io.github.aki0057.multitenant.auth.infrastructure.persistence.repository;

import io.github.aki0057.multitenant.auth.domain.model.RefreshToken;
import io.github.aki0057.multitenant.auth.domain.model.vo.TokenHash;
import io.github.aki0057.multitenant.auth.domain.repository.RefreshTokenRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@link RefreshTokenRepository} の一時スタブアダプタ。
 * DI コンテナが具象 Bean を解決できるようにし、@SpringBootTest のコンテキスト起動を通すためだけに存在する。
 * JPA による永続化の実装は持たない。
 */
// TODO: JPA 実装へ置換（後続 infrastructure 増分）
@Repository
public class StubRefreshTokenRepository implements RefreshTokenRepository {

    /**
     * 未実装のスタブメソッド。
     *
     * @param tokenHash 生トークンの SHA-256 ハッシュ値
     * @return なし（常に {@link UnsupportedOperationException} をスローする）
     */
    @Override
    public Optional<RefreshToken> findByTokenHash(TokenHash tokenHash) {
        throw new UnsupportedOperationException("RefreshTokenRepository#findByTokenHash は未実装です");
    }

    /**
     * 未実装のスタブメソッド。
     *
     * @param refreshToken 保存するリフレッシュトークン
     * @return なし（常に {@link UnsupportedOperationException} をスローする）
     */
    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        throw new UnsupportedOperationException("RefreshTokenRepository#save は未実装です");
    }
}
