package io.github.aki0057.multitenant.auth.infrastructure.security;

import io.github.aki0057.multitenant.auth.domain.model.vo.PasswordHash;
import io.github.aki0057.multitenant.auth.domain.model.vo.RawPassword;
import io.github.aki0057.multitenant.auth.domain.service.PasswordVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * {@link PasswordVerifier} の実装アダプタ。
 * Spring Security の {@link PasswordEncoder}（BCrypt）へ照合処理を委譲する。
 */
@Component
@RequiredArgsConstructor
public class PasswordEncoderVerifier implements PasswordVerifier {

    private final PasswordEncoder passwordEncoder;

    /**
     * 生パスワードがハッシュ化済みパスワードと一致するかを検証する。
     *
     * @param rawPassword  照合対象の生パスワード
     * @param passwordHash 比較対象のハッシュ化済みパスワード
     * @return 一致する場合 true
     */
    @Override
    public boolean matches(RawPassword rawPassword, PasswordHash passwordHash) {
        return passwordEncoder.matches(rawPassword.value(), passwordHash.value());
    }
}
