package io.github.aki0057.multitenant.auth.domain.model;

import io.github.aki0057.multitenant.auth.domain.exception.AuthenticationFailedException;
import io.github.aki0057.multitenant.auth.domain.model.vo.*;
import io.github.aki0057.multitenant.auth.domain.service.PasswordVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final RawPassword RAW_PASSWORD = new RawPassword("password");

    /** パスワードが常に一致するスタブ。 */
    private static final PasswordVerifier MATCHING_VERIFIER = (raw, hash) -> true;

    /** パスワードが常に不一致のスタブ。 */
    private static final PasswordVerifier NON_MATCHING_VERIFIER = (raw, hash) -> false;

    private User newUser(boolean userIdIsActive, boolean tenantIdIsActive) {
        return new User(
                new UserId(1L),
                new TenantId(1L),
                new TenantCode("testTenant"),
                new Email("test@example.com"),
                new PasswordHash("hashed-pass"),
                new Role("USER"),
                userIdIsActive,
                tenantIdIsActive
        );
    }

    // ---------------------------------------------------------------
    // 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: アカウント・テナントが有効でパスワードが一致する場合、例外はスローされない。")
    void authenticate_success() {
        User user = newUser(true, true);

        assertThatCode(() -> user.authenticate(RAW_PASSWORD, MATCHING_VERIFIER))
                .doesNotThrowAnyException();
    }

    // ---------------------------------------------------------------
    // 異常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("異常系: アカウントが無効（userIdIsActive=false）の場合は AuthenticationFailedException がスローされる。")
    void authenticate_userInactive() {
        User user = newUser(false, true);

        assertThatThrownBy(() -> user.authenticate(RAW_PASSWORD, MATCHING_VERIFIER))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("異常系: テナントが無効（tenantIdIsActive=false）の場合は AuthenticationFailedException がスローされる。")
    void authenticate_tenantInactive() {
        User user = newUser(true, false);

        assertThatThrownBy(() -> user.authenticate(RAW_PASSWORD, MATCHING_VERIFIER))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("異常系: パスワードが一致しない場合は AuthenticationFailedException がスローされる。")
    void authenticate_wrongPassword() {
        User user = newUser(true, true);

        assertThatThrownBy(() -> user.authenticate(RAW_PASSWORD, NON_MATCHING_VERIFIER))
                .isInstanceOf(AuthenticationFailedException.class);
    }
}
