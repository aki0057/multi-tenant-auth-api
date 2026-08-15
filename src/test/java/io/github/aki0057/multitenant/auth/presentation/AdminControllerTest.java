package io.github.aki0057.multitenant.auth.presentation;

import io.github.aki0057.multitenant.auth.application.ListTenantUsersCommand;
import io.github.aki0057.multitenant.auth.application.TenantUserResult;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class, GlobalExceptionHandler.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    /** SecurityConfig#defaultSecurityFilterChain が要求する依存を満たすためのモック。 */
    @MockitoBean
    private AccessTokenVerifier accessTokenVerifier;

    /**
     * JwtAuthenticationFilter が SecurityContext にセットするのと同じ形の認証情報を組み立てる。
     * principal はテナント ID 2 に所属する ADMIN ロールのユーザーとする。
     *
     * @return {@link AuthenticatedUser} を principal に持つ認証済みトークン
     */
    private Authentication adminToken() {
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(new UserId(1L), new TenantId(2L), new Role("ADMIN"));
        return new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                List.of(new SimpleGrantedAuthority("ADMIN")));
    }

    /**
     * JwtAuthenticationFilter が SecurityContext にセットするのと同じ形の認証情報を組み立てる。
     * principal はテナント ID 2 に所属する USER ロールのユーザーとする。
     *
     * @return {@link AuthenticatedUser}（USER ロール）を principal に持つ認証済みトークン
     */
    private Authentication userToken() {
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(new UserId(3L), new TenantId(2L), new Role("USER"));
        return new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                List.of(new SimpleGrantedAuthority("USER")));
    }

    @Test
    @DisplayName("正常系: ADMIN が GET /admin/users を呼ぶと 200 OK・素の配列で id / email / role / isActive が返る。")
    void listTenantUsers_success() throws Exception {
        when(userService.listTenantUsers(any()))
                .thenReturn(List.of(new TenantUserResult(1L, "admin@example.com", "ADMIN", true)));

        mockMvc.perform(get("/admin/users")
                        .with(authentication(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("admin@example.com"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    @DisplayName("正常系: application 層が返した id 昇順の並びがそのまま維持される。")
    void listTenantUsers_keepsIdAscendingOrder() throws Exception {
        when(userService.listTenantUsers(any()))
                .thenReturn(List.of(
                        new TenantUserResult(1L, "admin@example.com", "ADMIN", true),
                        new TenantUserResult(2L, "user2@example.com", "USER", true),
                        new TenantUserResult(3L, "user3@example.com", "USER", true)));

        mockMvc.perform(get("/admin/users")
                        .with(authentication(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[2].id").value(3));
    }

    @Test
    @DisplayName("正常系: 無効ユーザーも一覧に含まれ isActive が false で返る。")
    void listTenantUsers_includesInactiveUser() throws Exception {
        when(userService.listTenantUsers(any()))
                .thenReturn(List.of(
                        new TenantUserResult(1L, "active@example.com", "USER", true),
                        new TenantUserResult(2L, "inactive@example.com", "USER", false)));

        mockMvc.perform(get("/admin/users")
                        .with(authentication(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].email").value("inactive@example.com"))
                .andExpect(jsonPath("$[1].isActive").value(false));
    }

    @Test
    @DisplayName("正常系: 管理者本人も一覧に含まれる。")
    void listTenantUsers_includesAdminSelf() throws Exception {
        when(userService.listTenantUsers(any()))
                .thenReturn(List.of(
                        new TenantUserResult(1L, "admin@example.com", "ADMIN", true),
                        new TenantUserResult(2L, "user2@example.com", "USER", true)));

        mockMvc.perform(get("/admin/users")
                        .with(authentication(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // principal のユーザー ID（1）と同じ管理者本人が除外されずに含まれる
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("admin@example.com"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }

    @Test
    @DisplayName("正常系: principal のテナント ID が ListTenantUsersCommand として application 層へ渡る。")
    void listTenantUsers_passesPrincipalTenantIdToCommand() throws Exception {
        when(userService.listTenantUsers(any()))
                .thenReturn(List.of(new TenantUserResult(1L, "admin@example.com", "ADMIN", true)));

        mockMvc.perform(get("/admin/users")
                        .with(authentication(adminToken())))
                .andExpect(status().isOk());

        ArgumentCaptor<ListTenantUsersCommand> captor =
                ArgumentCaptor.forClass(ListTenantUsersCommand.class);
        verify(userService).listTenantUsers(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("正常系: 該当ユーザーが 0 件の場合は 404 ではなく 200 OK・空配列が返る。")
    void listTenantUsers_empty() throws Exception {
        when(userService.listTenantUsers(any())).thenReturn(List.of());

        mockMvc.perform(get("/admin/users")
                        .with(authentication(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("異常系: 未認証（アクセストークンなし）で GET /admin/users を呼ぶと 401 Unauthorized が返る。")
    void listTenantUsers_unauthenticated() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("異常系: USER ロールで GET /admin/users を呼ぶと 403 ではなく 404 Not Found が返る。")
    void listTenantUsers_forbiddenForUserRole() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .with(authentication(userToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));

        verify(userService, never()).listTenantUsers(any());
    }
}
