package io.github.aki0057.multitenant.auth.application;

import io.github.aki0057.multitenant.auth.domain.exception.AuthenticationFailedException;
import io.github.aki0057.multitenant.auth.domain.exception.InvalidRefreshTokenException;
import io.github.aki0057.multitenant.auth.domain.model.RefreshToken;
import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.Email;
import io.github.aki0057.multitenant.auth.domain.model.vo.RawPassword;
import io.github.aki0057.multitenant.auth.domain.model.vo.RawRefreshToken;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantCode;
import io.github.aki0057.multitenant.auth.domain.model.vo.TokenHash;
import io.github.aki0057.multitenant.auth.domain.repository.RefreshTokenRepository;
import io.github.aki0057.multitenant.auth.domain.repository.UserRepository;
import io.github.aki0057.multitenant.auth.domain.service.AccessTokenProvider;
import io.github.aki0057.multitenant.auth.domain.service.PasswordVerifier;
import io.github.aki0057.multitenant.auth.domain.service.RefreshTokenExpirationPolicy;
import io.github.aki0057.multitenant.auth.domain.service.RefreshTokenGenerator;
import io.github.aki0057.multitenant.auth.domain.service.RefreshTokenHasher;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

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
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenHasher refreshTokenHasher;
    private final RefreshTokenExpirationPolicy refreshTokenExpirationPolicy;
    private final Clock clock;

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

    /**
     * リフレッシュ処理。
     * 提示されたリフレッシュトークンを検証し、アクセストークンを再発行するとともに
     * リフレッシュトークンをローテーションする。
     *
     * <p>提示された生トークンをハッシュ化して該当レコードを検索し、
     * 失効・期限切れでないこと、所有ユーザーとそのテナントが有効であることを確認する。
     * 検証成功時は旧トークンを失効させ、新しいトークンを発行・保存する
     * （旧トークン失効と新トークン保存は同一トランザクションで原子的に行う）。</p>
     *
     * <p>トークン不存在・期限切れ・失効済み・ユーザー無効・テナント無効のいずれの場合も、
     * 原因を外部へ露出させないよう一律に {@link BadCredentialsException}（HTTP 401 相当）へ変換する。</p>
     *
     * @param command リフレッシュコマンド
     * @return 再発行されたアクセストークンと新しいリフレッシュトークンを含む {@link RefreshResult}
     * @throws BadCredentialsException リフレッシュトークンが無効（不存在・期限切れ・失効済み・
     *         ユーザー無効・テナント無効のいずれか）の場合
     */
    @Transactional
    public RefreshResult refresh(@NonNull RefreshCommand command) {
        try {
            // クライアントから送られたリフレッシュトークンをハッシュ化した値でDBを検索
            RawRefreshToken rawRefreshToken = new RawRefreshToken(command.refreshToken());
            TokenHash tokenHash = refreshTokenHasher.hash(rawRefreshToken);

            RefreshToken oldToken = refreshTokenRepository.findByTokenHash(tokenHash)
                    .orElseThrow(InvalidRefreshTokenException::new);

            Instant now = clock.instant();
            if (!oldToken.isValid(now)) {
                throw new InvalidRefreshTokenException();
            }

            User user = userRepository.findById(oldToken.userId())
                    .orElseThrow(InvalidRefreshTokenException::new);
            if (!user.isActive()) {
                throw new InvalidRefreshTokenException();
            }

            // ローテーション: 旧トークンを失効状態で保存する
            refreshTokenRepository.save(oldToken.revoke());

            // 新しいトークンを生成・保存する
            RawRefreshToken newRawToken = refreshTokenGenerator.generate();
            TokenHash newTokenHash = refreshTokenHasher.hash(newRawToken);
            refreshTokenRepository.save(new RefreshToken(
                    null, user.tenantId(), user.userId(), newTokenHash,
                    now.plus(refreshTokenExpirationPolicy.expiration()), false));

            String accessToken = accessTokenProvider.issue(user);
            return new RefreshResult(accessToken, newRawToken.value());
        } catch (InvalidRefreshTokenException e) {
            // ドメインの検証失敗を、認証 API の共通レスポンス（401）へ変換する
            throw new BadCredentialsException("Invalid refresh token");
        }
    }
}
