package io.github.aki0057.multitenant.auth.presentation;

import io.github.aki0057.multitenant.auth.application.ListTenantUsersCommand;
import io.github.aki0057.multitenant.auth.application.UserService;
import io.github.aki0057.multitenant.auth.domain.model.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    /**
     * ログイン中の管理者と同一テナントに所属するユーザーの一覧を返すエンドポイント。
     * {@code JwtAuthenticationFilter} が SecurityContext にセットした
     * {@link AuthenticatedUser} からテナント ID を取り出して application 層へ渡し、
     * 該当テナントのユーザーを JSON 配列で返す。
     *
     * <p>レスポンスのトップレベルはラップせず素の配列とし、application 層が返した
     * {@code TenantUserResult} を 1:1 で {@link TenantUserResponse} へ詰め替える。
     * 並び順は ID 昇順であり、並び替えの責務は application 層以降にあるため、
     * 本メソッドは返ってきた順序をそのまま維持する。
     * 該当ユーザーが 1 件も存在しない場合も 404 ではなく 200 OK + 空配列を返す。</p>
     *
     * <p><strong>無効ユーザー（{@code users.is_active = false}）も一覧に含める。</strong>
     * 管理者が自テナントのユーザー状態を把握する用途のため、無効ユーザーを 404 相当として扱う
     * {@code UserController#getMe} とは意図的に方針が異なる。
     * 有効・無効の状態は {@link TenantUserResponse#isActive()} で返す。
     * 管理者本人も一覧から除外しない。</p>
     *
     * <p>ADMIN ロールかどうかの認可判定は {@code SecurityConfig} の認可設定に寄せるため、
     * 本メソッドには認可アノテーションを付けない。
     * 未認証・不正／期限切れトークンの場合は本メソッドに到達せず、
     * {@code SecurityConfig} の {@code authenticationEntryPoint} により 401 Unauthorized が返る。</p>
     *
     * @param authenticatedUser アクセストークンから復元された認証済みユーザー
     * @return 同一テナントに所属するユーザーを ID 昇順で並べた {@link TenantUserResponse} の配列（200 OK）。
     *         該当ユーザーが存在しない場合は空配列
     */
    @Operation(
            summary = "同一テナントのユーザー一覧の取得",
            description = "アクセストークンで認証された管理者と同一テナントに所属するユーザーの一覧を"
                    + "ID 昇順で返す。認証必須（Authorization: Bearer <アクセストークン>）の"
                    + "エンドポイント。無効ユーザーおよび管理者本人も一覧に含み、"
                    + "該当ユーザーが存在しない場合は空配列を返す。")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/admin/users")
    public ResponseEntity<List<TenantUserResponse>> listTenantUsers(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(userService.listTenantUsers(
                        new ListTenantUsersCommand(authenticatedUser.tenantId().value()))
                .stream()
                .map(result -> new TenantUserResponse(
                        result.id(), result.email(), result.role(), result.isActive()))
                .toList());
    }
}
