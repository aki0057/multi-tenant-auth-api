package io.github.aki0057.multitenant.auth.infrastructure.security;

import io.github.aki0057.multitenant.auth.domain.model.AuthenticatedUser;
import io.github.aki0057.multitenant.auth.domain.model.vo.Role;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantId;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;
import io.github.aki0057.multitenant.auth.domain.service.AccessTokenVerifier;
import org.springframework.stereotype.Component;

/**
 * {@link AccessTokenVerifier} の一時スタブアダプタ。
 *
 * <p>{@code @SpringBootTest} によるフルコンテキスト起動が具象 Bean を要求するために存在する。
 * 中身は未実装であり、増分2で {@code JwtAccessTokenVerifier} 実装へ置換する。</p>
 */
@Component
public class StubAccessTokenVerifier implements AccessTokenVerifier {

    @Override
    public AuthenticatedUser verify(String token) {
        // TODO: 増分2で jjwt による HS256 検証とクレーム復元を実装する
        return new AuthenticatedUser(new UserId(1L), new TenantId(1L), new Role("USER"));
    }
}
