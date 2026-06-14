package io.github.aki0057.multitenant.auth.application;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.Email;
import io.github.aki0057.multitenant.auth.domain.model.vo.PasswordHash;
import io.github.aki0057.multitenant.auth.domain.model.vo.Role;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantCode;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;
import io.github.aki0057.multitenant.auth.domain.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private static final LoginCommand COMMAND =
            new LoginCommand("test-tenant", "test@example.com", "password");

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new User(
                new UserId(1L),
                new TenantCode("test-tenant"),
                new Email("test@example.com"),
                new PasswordHash("hashed-pass"),
                new Role("USER"),
                true
        );
    }

    // ---------------------------------------------------------------
    // 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: ユーザーが存在し、アカウントが有効で、パスワードが一致する場合は例外がスローされない。")
    void login_success() {
        when(userRepository.findByTenantCodeAndEmail(any(), any()))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password", "hashed-pass"))
                .thenReturn(true);

        assertThatCode(() -> authService.login(COMMAND))
                .doesNotThrowAnyException();
    }

    // ---------------------------------------------------------------
    // 異常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("異常系: ユーザーが存在しない場合は BadCredentialsException がスローされる。")
    void login_userNotFound() {
        when(userRepository.findByTenantCodeAndEmail(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(COMMAND))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("異常系: アカウントが無効（active=false）の場合は BadCredentialsException がスローされる。")
    void login_accountInactive() {
        User inactiveUser = new User(
                new UserId(1L),
                new TenantCode("test-tenant"),
                new Email("test@example.com"),
                new PasswordHash("hashed-pass"),
                new Role("USER"),
                false
        );
        when(userRepository.findByTenantCodeAndEmail(any(), any()))
                .thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> authService.login(COMMAND))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("異常系: パスワードが一致しない場合は BadCredentialsException がスローされる。")
    void login_wrongPassword() {
        when(userRepository.findByTenantCodeAndEmail(any(), any()))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password", "hashed-pass"))
                .thenReturn(false);  // パスワード不一致

        assertThatThrownBy(() -> authService.login(COMMAND))
                .isInstanceOf(BadCredentialsException.class);
    }
}

