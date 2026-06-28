package io.github.aki0057.multitenant.auth.application;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.Email;
import io.github.aki0057.multitenant.auth.domain.model.vo.RawPassword;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantCode;
import io.github.aki0057.multitenant.auth.domain.repository.UserRepository;
import io.github.aki0057.multitenant.auth.domain.service.AccessTokenProvider;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
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
    private final AccessTokenProvider accessTokenProvider;

    /**
     * ログイン処理。
     * テナントコード・メールアドレスでユーザーを検索し、パスワードを照合する。
     * 認証に成功した場合は JWT アクセストークンを返す。
     *
     * @param command ログインコマンド
     * @return 発行された JWT アクセストークン
     * @throws BadCredentialsException ユーザーが存在しない / パスワード不一致 / アカウント無効の場合
     */
    @Transactional(readOnly = true)
    public String login(@NonNull LoginCommand command) {
        final TenantCode tenantCode = new TenantCode(command.tenantCode());
        final Email email = new Email(command.email());
        final RawPassword rawPassword = new RawPassword(command.password());

        User user = userRepository
                .findByTenantCodeAndEmail(tenantCode, email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.userIdIsActive()) {
            // userが存在してもアカウントが無効な場合は認証失敗とする
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.tenantIdIsActive()) {
            // tenantが存在してもアカウントが無効な場合は認証失敗とする
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!passwordEncoder.matches(rawPassword.value(), user.passwordHash().value())) {
            // パスワード不一致も認証失敗とする
            throw new BadCredentialsException("Invalid credentials");
        }

        return accessTokenProvider.issue(user);
    }
}

