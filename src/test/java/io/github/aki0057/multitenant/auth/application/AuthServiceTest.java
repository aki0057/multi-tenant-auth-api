package io.github.aki0057.multitenant.auth.application;

import io.github.aki0057.multitenant.auth.domain.model.RefreshToken;
import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.*;
import io.github.aki0057.multitenant.auth.domain.repository.RefreshTokenRepository;
import io.github.aki0057.multitenant.auth.domain.repository.UserRepository;
import io.github.aki0057.multitenant.auth.domain.service.AccessTokenProvider;
import io.github.aki0057.multitenant.auth.domain.service.PasswordVerifier;
import io.github.aki0057.multitenant.auth.domain.service.RefreshTokenExpirationPolicy;
import io.github.aki0057.multitenant.auth.domain.service.RefreshTokenGenerator;
import io.github.aki0057.multitenant.auth.domain.service.RefreshTokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordVerifier passwordVerifier;

    @Mock
    private AccessTokenProvider accessTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    @Mock
    private RefreshTokenHasher refreshTokenHasher;

    @Mock
    private RefreshTokenExpirationPolicy refreshTokenExpirationPolicy;

    @Mock
    private Clock clock;

    @InjectMocks
    private AuthService authService;

    private static final LoginCommand COMMAND =
            new LoginCommand("testTenant", "test@example.com", "password");

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new User(
                new UserId(1L),
                new TenantId(1L),
                new TenantCode("testTenant"),
                new Email("test@example.com"),
                new PasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"),
                new Role("USER"),
                true,
                true
        );
    }

    // ===============================================================
    // login
    // ===============================================================

    private static final RawRefreshToken LOGIN_RAW_TOKEN = new RawRefreshToken("login-raw-token");
    private static final TokenHash LOGIN_TOKEN_HASH = new TokenHash("c".repeat(64));

    // ---------------------------------------------------------------
    // login 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: 認証に成功した場合、アクセストークンと新規発行のリフレッシュトークンを含む LoginResult を返す。")
    void login_success() {
        when(userRepository.findByTenantCodeAndEmail(any(), any()))
                .thenReturn(Optional.of(activeUser));
        when(passwordVerifier.matches(any(), any()))
                .thenReturn(true);
        when(accessTokenProvider.issue(activeUser))
                .thenReturn("issued-access-token");
        when(clock.instant()).thenReturn(FIXED_NOW);
        when(refreshTokenGenerator.generate()).thenReturn(LOGIN_RAW_TOKEN);
        when(refreshTokenHasher.hash(LOGIN_RAW_TOKEN)).thenReturn(LOGIN_TOKEN_HASH);
        when(refreshTokenExpirationPolicy.expiration()).thenReturn(Duration.ofDays(14));

        LoginResult result = authService.login(COMMAND);

        assertThat(result.accessToken()).isEqualTo("issued-access-token");
        assertThat(result.refreshToken()).isEqualTo("login-raw-token");
    }

    @Test
    @DisplayName("正常系: 認証成功時、生成・ハッシュ化した新規リフレッシュトークンが失効なし・null ID で保存される。")
    void login_savesNewlyIssuedToken() {
        when(userRepository.findByTenantCodeAndEmail(any(), any()))
                .thenReturn(Optional.of(activeUser));
        when(passwordVerifier.matches(any(), any()))
                .thenReturn(true);
        when(accessTokenProvider.issue(activeUser))
                .thenReturn("issued-access-token");
        when(clock.instant()).thenReturn(FIXED_NOW);
        when(refreshTokenGenerator.generate()).thenReturn(LOGIN_RAW_TOKEN);
        when(refreshTokenHasher.hash(LOGIN_RAW_TOKEN)).thenReturn(LOGIN_TOKEN_HASH);
        when(refreshTokenExpirationPolicy.expiration()).thenReturn(Duration.ofDays(14));

        authService.login(COMMAND);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        // 新規発行のみ（ローテーションではない）ため保存は 1 回だけ
        verify(refreshTokenRepository, times(1)).save(captor.capture());

        RefreshToken savedToken = captor.getValue();
        assertThat(savedToken.id()).isNull();
        assertThat(savedToken.tenantId()).isEqualTo(activeUser.tenantId());
        assertThat(savedToken.userId()).isEqualTo(activeUser.userId());
        assertThat(savedToken.tokenHash()).isEqualTo(LOGIN_TOKEN_HASH);
        assertThat(savedToken.expiresAt()).isEqualTo(FIXED_NOW.plus(Duration.ofDays(14)));
        assertThat(savedToken.revoked()).isFalse();
    }

    // ---------------------------------------------------------------
    // login 異常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("異常系: ユーザーが存在しない場合は BadCredentialsException がスローされ、トークンは発行・保存されない。")
    void login_userNotFound() {
        when(userRepository.findByTenantCodeAndEmail(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(COMMAND))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("異常系: User の認証が失敗した場合は BadCredentialsException に変換され、トークンは発行・保存されない。")
    void login_authenticationFailed() {
        when(userRepository.findByTenantCodeAndEmail(any(), any()))
                .thenReturn(Optional.of(activeUser));
        when(passwordVerifier.matches(any(), any()))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.login(COMMAND))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("異常系: tenantCode が形式不正（記号を含む）な場合は BadCredentialsException に変換され、トークンは発行・保存されない。")
    void login_invalidTenantCodeFormat() {
        LoginCommand command =
                new LoginCommand("bad!tenant", "test@example.com", "password");

        assertThatThrownBy(() -> authService.login(command))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("異常系: email が形式不正（ドット無し a@b）な場合は BadCredentialsException に変換され、トークンは発行・保存されない。")
    void login_invalidEmailFormat() {
        LoginCommand command =
                new LoginCommand("testTenant", "a@b", "password");

        assertThatThrownBy(() -> authService.login(command))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("異常系: password が形式不正（8 文字未満）な場合は BadCredentialsException に変換され、トークンは発行・保存されない。")
    void login_invalidPasswordFormat() {
        LoginCommand command =
                new LoginCommand("testTenant", "test@example.com", "short");

        assertThatThrownBy(() -> authService.login(command))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    // ===============================================================
    // refresh
    // ===============================================================

    private static final Instant FIXED_NOW = Instant.parse("2026-07-03T00:00:00Z");

    private static final RefreshCommand REFRESH_COMMAND =
            new RefreshCommand("old-raw-token");

    private static final RawRefreshToken OLD_RAW_TOKEN = new RawRefreshToken("old-raw-token");
    private static final TokenHash OLD_TOKEN_HASH = new TokenHash("a".repeat(64));
    private static final RawRefreshToken NEW_RAW_TOKEN = new RawRefreshToken("new-raw-token");
    private static final TokenHash NEW_TOKEN_HASH = new TokenHash("b".repeat(64));

    /**
     * 未失効・未期限切れの有効な旧リフレッシュトークンを生成する。
     */
    private RefreshToken validOldToken() {
        return new RefreshToken(
                new RefreshTokenId(10L),
                new TenantId(1L),
                new UserId(1L),
                OLD_TOKEN_HASH,
                FIXED_NOW.plus(Duration.ofDays(1)),
                false
        );
    }

    // ---------------------------------------------------------------
    // refresh 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: 有効なリフレッシュトークンの場合、新アクセストークンと新リフレッシュトークンを返し、旧トークンの失効と新トークンの保存を行う。")
    void refresh_success() {
        when(refreshTokenHasher.hash(OLD_RAW_TOKEN)).thenReturn(OLD_TOKEN_HASH);
        when(refreshTokenRepository.findByTokenHash(OLD_TOKEN_HASH))
                .thenReturn(Optional.of(validOldToken()));
        when(clock.instant()).thenReturn(FIXED_NOW);
        when(userRepository.findById(new UserId(1L)))
                .thenReturn(Optional.of(activeUser));
        when(refreshTokenGenerator.generate()).thenReturn(NEW_RAW_TOKEN);
        when(refreshTokenHasher.hash(NEW_RAW_TOKEN)).thenReturn(NEW_TOKEN_HASH);
        when(refreshTokenExpirationPolicy.expiration()).thenReturn(Duration.ofDays(14));
        when(accessTokenProvider.issue(activeUser)).thenReturn("new-access-token");

        RefreshResult result = authService.refresh(REFRESH_COMMAND);

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-raw-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());

        RefreshToken revokedOldToken = captor.getAllValues().get(0);
        assertThat(revokedOldToken.id()).isEqualTo(new RefreshTokenId(10L));
        assertThat(revokedOldToken.userId()).isEqualTo(new UserId(1L));
        assertThat(revokedOldToken.tokenHash()).isEqualTo(OLD_TOKEN_HASH);
        assertThat(revokedOldToken.expiresAt()).isEqualTo(FIXED_NOW.plus(Duration.ofDays(1)));
        assertThat(revokedOldToken.revoked()).isTrue();

        RefreshToken savedNewToken = captor.getAllValues().get(1);
        assertThat(savedNewToken.id()).isNull();
        assertThat(savedNewToken.tenantId()).isEqualTo(activeUser.tenantId());
        assertThat(savedNewToken.userId()).isEqualTo(new UserId(1L));
        assertThat(savedNewToken.tokenHash()).isEqualTo(NEW_TOKEN_HASH);
        assertThat(savedNewToken.expiresAt()).isEqualTo(FIXED_NOW.plus(Duration.ofDays(14)));
        assertThat(savedNewToken.revoked()).isFalse();
    }

    // ---------------------------------------------------------------
    // refresh 異常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("異常系: リフレッシュトークンが見つからない場合は BadCredentialsException がスローされ、トークンは発行されない。")
    void refresh_tokenNotFound() {
        when(refreshTokenHasher.hash(OLD_RAW_TOKEN)).thenReturn(OLD_TOKEN_HASH);
        when(refreshTokenRepository.findByTokenHash(OLD_TOKEN_HASH))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(REFRESH_COMMAND))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
    }

    @Test
    @DisplayName("異常系: リフレッシュトークンが失効済みの場合は BadCredentialsException がスローされ、トークンは発行されない。")
    void refresh_tokenRevoked() {
        RefreshToken revokedToken = new RefreshToken(
                new RefreshTokenId(10L),
                new TenantId(1L),
                new UserId(1L),
                OLD_TOKEN_HASH,
                FIXED_NOW.plus(Duration.ofDays(1)),
                true
        );
        when(refreshTokenHasher.hash(OLD_RAW_TOKEN)).thenReturn(OLD_TOKEN_HASH);
        when(refreshTokenRepository.findByTokenHash(OLD_TOKEN_HASH))
                .thenReturn(Optional.of(revokedToken));
        when(clock.instant()).thenReturn(FIXED_NOW);

        assertThatThrownBy(() -> authService.refresh(REFRESH_COMMAND))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
    }

    @Test
    @DisplayName("異常系: リフレッシュトークンが期限切れの場合は BadCredentialsException がスローされ、トークンは発行されない。")
    void refresh_tokenExpired() {
        RefreshToken expiredToken = new RefreshToken(
                new RefreshTokenId(10L),
                new TenantId(1L),
                new UserId(1L),
                OLD_TOKEN_HASH,
                FIXED_NOW.minus(Duration.ofSeconds(1)),
                false
        );
        when(refreshTokenHasher.hash(OLD_RAW_TOKEN)).thenReturn(OLD_TOKEN_HASH);
        when(refreshTokenRepository.findByTokenHash(OLD_TOKEN_HASH))
                .thenReturn(Optional.of(expiredToken));
        when(clock.instant()).thenReturn(FIXED_NOW);

        assertThatThrownBy(() -> authService.refresh(REFRESH_COMMAND))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
    }

    @Test
    @DisplayName("異常系: トークンの所有ユーザーが存在しない場合は BadCredentialsException がスローされ、トークンは発行されない。")
    void refresh_userNotFound() {
        when(refreshTokenHasher.hash(OLD_RAW_TOKEN)).thenReturn(OLD_TOKEN_HASH);
        when(refreshTokenRepository.findByTokenHash(OLD_TOKEN_HASH))
                .thenReturn(Optional.of(validOldToken()));
        when(clock.instant()).thenReturn(FIXED_NOW);
        when(userRepository.findById(new UserId(1L)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(REFRESH_COMMAND))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
    }

    @Test
    @DisplayName("異常系: トークンの所有ユーザーが無効の場合は BadCredentialsException がスローされ、トークンは発行されない。")
    void refresh_userInactive() {
        User inactiveUser = new User(
                new UserId(1L),
                new TenantId(1L),
                new TenantCode("testTenant"),
                new Email("test@example.com"),
                new PasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"),
                new Role("USER"),
                false,
                true
        );
        when(refreshTokenHasher.hash(OLD_RAW_TOKEN)).thenReturn(OLD_TOKEN_HASH);
        when(refreshTokenRepository.findByTokenHash(OLD_TOKEN_HASH))
                .thenReturn(Optional.of(validOldToken()));
        when(clock.instant()).thenReturn(FIXED_NOW);
        when(userRepository.findById(new UserId(1L)))
                .thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> authService.refresh(REFRESH_COMMAND))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
    }

    @Test
    @DisplayName("異常系: トークンの所有ユーザーのテナントが無効の場合は BadCredentialsException がスローされ、トークンは発行されない。")
    void refresh_tenantInactive() {
        User tenantInactiveUser = new User(
                new UserId(1L),
                new TenantId(1L),
                new TenantCode("testTenant"),
                new Email("test@example.com"),
                new PasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"),
                new Role("USER"),
                true,
                false
        );
        when(refreshTokenHasher.hash(OLD_RAW_TOKEN)).thenReturn(OLD_TOKEN_HASH);
        when(refreshTokenRepository.findByTokenHash(OLD_TOKEN_HASH))
                .thenReturn(Optional.of(validOldToken()));
        when(clock.instant()).thenReturn(FIXED_NOW);
        when(userRepository.findById(new UserId(1L)))
                .thenReturn(Optional.of(tenantInactiveUser));

        assertThatThrownBy(() -> authService.refresh(REFRESH_COMMAND))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
    }
}
