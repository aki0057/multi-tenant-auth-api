package io.github.aki0057.multitenant.auth.application;

import io.github.aki0057.multitenant.auth.domain.exception.AuthenticationFailedException;
import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.Email;
import io.github.aki0057.multitenant.auth.domain.model.vo.RawPassword;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantCode;
import io.github.aki0057.multitenant.auth.domain.repository.UserRepository;
import io.github.aki0057.multitenant.auth.domain.service.AccessTokenProvider;
import io.github.aki0057.multitenant.auth.domain.service.PasswordVerifier;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
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
    private final PasswordVerifier passwordVerifier;
    private final AccessTokenProvider accessTokenProvider;

    /**
     * ログイン処理。
     * テナントコード・メールアドレスでユーザーを検索し、認証を行う。
     * 認証に成功した場合は JWT アクセストークンを返す。
     *
     * <p>入力値（テナントコード・メールアドレス・パスワード）の形式が不正で
     * Value Object の生成に失敗した場合も、ユーザー不在・パスワード不一致と同様に
     * {@link BadCredentialsException} へ変換する（HTTP 401 として応答する）。
     * これにより認証エラーの原因を外部へ露出させない。</p>
     *
     * @param command ログインコマンド
     * @return 発行された JWT アクセストークン
     * @throws BadCredentialsException 入力値の形式が不正（テナントコード / メールアドレス /
     *         パスワードが Value Object の検証に失敗）の場合、
     *         またはユーザーが存在しない / パスワード不一致 / アカウント無効の場合
     */
    @Transactional(readOnly = true)
    public String login(@NonNull LoginCommand command) {
        final TenantCode tenantCode;
        final Email email;
        final RawPassword rawPassword;
        try {
            tenantCode = new TenantCode(command.tenantCode());
            email = new Email(command.email());
            rawPassword = new RawPassword(command.password());
        } catch (IllegalArgumentException e) {
            // VO の形式検証失敗を、認証 API の共通レスポンス（401）へ変換する
            throw new BadCredentialsException("Invalid credentials");
        }

        User user = userRepository
                .findByTenantCodeAndEmail(tenantCode, email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        try {
            user.authenticate(rawPassword, passwordVerifier);
        } catch (AuthenticationFailedException e) {
            // ドメインの認証失敗を、認証 API の共通レスポンスへ変換する
            throw new BadCredentialsException("Invalid credentials");
        }

        return accessTokenProvider.issue(user);
    }
}
