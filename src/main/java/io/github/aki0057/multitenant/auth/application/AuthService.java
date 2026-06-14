package io.github.aki0057.multitenant.auth.application;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.Email;
import io.github.aki0057.multitenant.auth.domain.model.vo.RawPassword;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantCode;
import io.github.aki0057.multitenant.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 認証ユースケースを担うサービスクラス。
 * ユーザーの存在確認・パスワード照合を行う。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * ログイン処理。
     * テナントコード・メールアドレスでユーザーを検索し、パスワードを照合する。
     *
     * @param command ログインコマンド
     * @throws BadCredentialsException ユーザーが存在しない / パスワード不一致 / アカウント無効の場合
     */
    @Transactional(readOnly = true)
    public void login(LoginCommand command) {
        TenantCode tenantCode = new TenantCode(command.tenantCode());
        Email email = new Email(command.email());
        RawPassword rawPassword = new RawPassword(command.password());

        User user = userRepository
                .findByTenantCodeAndEmail(tenantCode, email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.active()) {
            // userが存在してもアカウントが無効な場合は認証失敗とする
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!passwordEncoder.matches(rawPassword.value(), user.passwordHash().value())) {
            // パスワード不一致も認証失敗とする
            throw new BadCredentialsException("Invalid credentials");
        }
    }
}

