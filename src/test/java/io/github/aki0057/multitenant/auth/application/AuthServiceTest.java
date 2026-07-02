package io.github.aki0057.multitenant.auth.application;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.*;
import io.github.aki0057.multitenant.auth.domain.repository.UserRepository;
import io.github.aki0057.multitenant.auth.domain.service.AccessTokenProvider;
import io.github.aki0057.multitenant.auth.domain.service.PasswordVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    // ---------------------------------------------------------------
    // 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: 認証に成功した場合、AccessTokenProvider が発行したトークン文字列を返す。")
    void login_success() {
        when(userRepository.findByTenantCodeAndEmail(any(), any()))
                .thenReturn(Optional.of(activeUser));
        when(passwordVerifier.matches(any(), any()))
                .thenReturn(true);
        when(accessTokenProvider.issue(activeUser))
                .thenReturn("issued-access-token");

        String accessToken = authService.login(COMMAND);

        assertThat(accessToken).isEqualTo("issued-access-token");
    }

    // ---------------------------------------------------------------
    // 異常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("異常系: ユーザーが存在しない場合は BadCredentialsException がスローされ、トークンは発行されない。")
    void login_userNotFound() {
        when(userRepository.findByTenantCodeAndEmail(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(COMMAND))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
    }

    @Test
    @DisplayName("異常系: User の認証が失敗（AuthenticationFailedException）した場合は BadCredentialsException に変換され、トークンは発行されない。")
    void login_authenticationFailed() {
        when(userRepository.findByTenantCodeAndEmail(any(), any()))
                .thenReturn(Optional.of(activeUser));
        when(passwordVerifier.matches(any(), any()))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.login(COMMAND))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
    }

    @Test
    @DisplayName("異常系: tenantCode が形式不正（記号を含む）な場合は BadCredentialsException に変換され、トークンは発行されない。")
    void login_invalidTenantCodeFormat() {
        LoginCommand command =
                new LoginCommand("bad!tenant", "test@example.com", "password");

        assertThatThrownBy(() -> authService.login(command))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
    }

    @Test
    @DisplayName("異常系: email が形式不正（ドット無し a@b）な場合は BadCredentialsException に変換され、トークンは発行されない。")
    void login_invalidEmailFormat() {
        LoginCommand command =
                new LoginCommand("testTenant", "a@b", "password");

        assertThatThrownBy(() -> authService.login(command))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
    }

    @Test
    @DisplayName("異常系: password が形式不正（8 文字未満）な場合は BadCredentialsException に変換され、トークンは発行されない。")
    void login_invalidPasswordFormat() {
        LoginCommand command =
                new LoginCommand("testTenant", "test@example.com", "short");

        assertThatThrownBy(() -> authService.login(command))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
    }
}
