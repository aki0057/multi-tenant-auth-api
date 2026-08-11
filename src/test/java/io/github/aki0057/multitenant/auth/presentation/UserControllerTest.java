package io.github.aki0057.multitenant.auth.presentation;

import io.github.aki0057.multitenant.auth.application.GetMeCommand;
import io.github.aki0057.multitenant.auth.application.GetMeResult;
import io.github.aki0057.multitenant.auth.application.UserService;
import io.github.aki0057.multitenant.auth.config.PasswordEncoderConfig;
import io.github.aki0057.multitenant.auth.config.SecurityConfig;
import io.github.aki0057.multitenant.auth.domain.model.AuthenticatedUser;
import io.github.aki0057.multitenant.auth.domain.model.vo.Role;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantId;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;
import io.github.aki0057.multitenant.auth.domain.service.AccessTokenVerifier;
import io.github.aki0057.multitenant.auth.presentation.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    /** SecurityConfig#defaultSecurityFilterChain が要求する依存を満たすためのモック。 */
    @MockitoBean
    private AccessTokenVerifier accessTokenVerifier;

    /**
     * JwtAuthenticationFilter が SecurityContext にセットするのと同じ形の認証情報を組み立てる。
     *
     * @return {@link AuthenticatedUser} を principal に持つ認証済みトークン
     */
    private Authentication authenticatedToken() {
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(new UserId(1L), new TenantId(2L), new Role("USER"));
        return new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                List.of(new SimpleGrantedAuthority("USER")));
    }

    @Test
    @DisplayName("正常系: 認証済みユーザーが GET /users/me を呼ぶと 200 OK・email / role が返る。")
    void getMe_success() throws Exception {
        when(userService.getMe(any()))
                .thenReturn(Optional.of(new GetMeResult("user@example.com", "USER")));

        mockMvc.perform(get("/users/me")
                        .with(authentication(authenticatedToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("正常系: principal のユーザー ID・テナント ID が GetMeCommand として application 層へ渡る。")
    void getMe_passesPrincipalIdsToCommand() throws Exception {
        when(userService.getMe(any()))
                .thenReturn(Optional.of(new GetMeResult("user@example.com", "USER")));

        mockMvc.perform(get("/users/me")
                        .with(authentication(authenticatedToken())))
                .andExpect(status().isOk());

        ArgumentCaptor<GetMeCommand> captor = ArgumentCaptor.forClass(GetMeCommand.class);
        verify(userService).getMe(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(1L);
        assertThat(captor.getValue().tenantId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("異常系: 該当ユーザーが存在しない場合は 404 Not Found が返る。")
    void getMe_userNotFound() throws Exception {
        when(userService.getMe(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/me")
                        .with(authentication(authenticatedToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("異常系: 未認証（アクセストークンなし）で GET /users/me を呼ぶと 401 Unauthorized が返る。")
    void getMe_unauthenticated() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}
