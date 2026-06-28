package io.github.aki0057.multitenant.auth.application;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.*;
import io.github.aki0057.multitenant.auth.domain.repository.UserRepository;
import io.github.aki0057.multitenant.auth.domain.service.AccessTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private PasswordEncoder passwordEncoder;

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
                new PasswordHash("hashed-pass"),
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
        when(passwordEncoder.matches("password", "hashed-pass"))
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
    @DisplayName("異常系: アカウントが無効（userIdIsActive=false）の場合は BadCredentialsException がスローされ、トークンは発行されない。")
    void login_userAccountInactive() {
        User inactiveUser = new User(
                new UserId(1L),
                new TenantId(1L),
                new TenantCode("testTenant"),
                new Email("test@example.com"),
                new PasswordHash("hashed-pass"),
                new Role("USER"),
                false,
                true
        );
        when(userRepository.findByTenantCodeAndEmail(any(), any()))
                .thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> authService.login(COMMAND))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
    }

    @Test
    @DisplayName("異常系: アカウントが無効（tenantIdIsActive=false）の場合は BadCredentialsException がスローされ、トークンは発行されない。")
    void login_tenantAccountInactive() {
        User inactiveTenantUser = new User(
                new UserId(1L),
                new TenantId(1L),
                new TenantCode("testTenant"),
                new Email("test@example.com"),
                new PasswordHash("hashed-pass"),
                new Role("USER"),
                true,
                false
        );
        when(userRepository.findByTenantCodeAndEmail(any(), any()))
                .thenReturn(Optional.of(inactiveTenantUser));

        assertThatThrownBy(() -> authService.login(COMMAND))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
    }

    @Test
    @DisplayName("異常系: パスワードが一致しない場合は BadCredentialsException がスローされ、トークンは発行されない。")
    void login_wrongPassword() {
        when(userRepository.findByTenantCodeAndEmail(any(), any()))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password", "hashed-pass"))
                .thenReturn(false);  // パスワード不一致

        assertThatThrownBy(() -> authService.login(COMMAND))
                .isInstanceOf(BadCredentialsException.class);
        verify(accessTokenProvider, never()).issue(any());
    }
}
