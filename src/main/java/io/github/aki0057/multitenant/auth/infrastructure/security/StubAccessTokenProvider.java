package io.github.aki0057.multitenant.auth.infrastructure.security;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.service.AccessTokenProvider;
import org.springframework.stereotype.Component;

/**
 * {@link AccessTokenProvider} の一時スタブアダプタ。
 * DI コンテナが具象 Bean を解決できるようにし、@SpringBootTest のコンテキスト起動を通すためだけに存在する。
 * jjwt による署名・claim 構築などの実装は持たない。
 */
// TODO: jjwt 実装へ置換（後続 infrastructure 増分）
@Component
public class StubAccessTokenProvider implements AccessTokenProvider {

    /**
     * ダミーのアクセストークンを返すスタブ実装。
     *
     * @param user 認証済みユーザー
     * @return 空文字列（ダミー値）
     */
    @Override
    public String issue(User user) {
        return "";
    }
}
