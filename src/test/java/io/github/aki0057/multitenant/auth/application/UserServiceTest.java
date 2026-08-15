package io.github.aki0057.multitenant.auth.application;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.*;
import io.github.aki0057.multitenant.auth.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private static final GetMeCommand COMMAND = new GetMeCommand(1L, 1L);

    private static final ListTenantUsersCommand LIST_COMMAND = new ListTenantUsersCommand(1L);

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

    /**
     * 基準となる有効ユーザーの一部の値のみを差し替えたユーザーを生成する。
     *
     * @param tenantId        所属テナントの主キー値
     * @param userIdIsActive  ユーザー自身の有効性
     * @param tenantIdIsActive 所属テナントの有効性
     * @return 差し替え後のユーザー
     */
    private User userWith(long tenantId, boolean userIdIsActive, boolean tenantIdIsActive) {
        return new User(
                activeUser.userId(),
                new TenantId(tenantId),
                activeUser.tenantCode(),
                activeUser.email(),
                activeUser.passwordHash(),
                activeUser.role(),
                userIdIsActive,
                tenantIdIsActive
        );
    }

    /**
     * テナント ID 1 に所属するユーザーを、一覧検証用に任意の値で生成する。
     *
     * @param userId           ユーザーの主キー値
     * @param email            ユーザーのメールアドレス
     * @param role             ユーザーのロール
     * @param userIdIsActive   ユーザー自身の有効性
     * @param tenantIdIsActive 所属テナントの有効性
     * @return 生成したユーザー
     */
    private User tenantUser(long userId, String email, String role,
                            boolean userIdIsActive, boolean tenantIdIsActive) {
        return new User(
                new UserId(userId),
                new TenantId(1L),
                activeUser.tenantCode(),
                new Email(email),
                activeUser.passwordHash(),
                new Role(role),
                userIdIsActive,
                tenantIdIsActive
        );
    }

    // ===============================================================
    // getMe
    // ===============================================================

    // ---------------------------------------------------------------
    // getMe 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: 有効なユーザーかつテナント ID が一致する場合、DB 由来のメールアドレスとロールを含む GetMeResult を返す。")
    void getMe_success() {
        when(userRepository.findById(new UserId(1L))).thenReturn(Optional.of(activeUser));

        Optional<GetMeResult> result = userService.getMe(COMMAND);

        assertThat(result).isPresent();
        assertThat(result.get().email()).isEqualTo("test@example.com");
        assertThat(result.get().role()).isEqualTo("USER");
    }

    @Test
    @DisplayName("正常系: コマンドのユーザー ID が UserId に変換されてリポジトリへ渡される。")
    void getMe_convertsUserIdToValueObject() {
        when(userRepository.findById(new UserId(1L))).thenReturn(Optional.of(activeUser));

        userService.getMe(COMMAND);

        verify(userRepository).findById(new UserId(1L));
    }

    // ---------------------------------------------------------------
    // getMe 異常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("異常系: 該当ユーザーが存在しない場合、例外を投げず空の Optional を返す。")
    void getMe_userNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThatCode(() -> assertThat(userService.getMe(COMMAND)).isEmpty())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("異常系: 該当ユーザーの所属テナントが入力のテナント ID と一致しない場合、例外を投げず空の Optional を返す。")
    void getMe_tenantMismatch() {
        when(userRepository.findById(any())).thenReturn(Optional.of(userWith(2L, true, true)));

        assertThatCode(() -> assertThat(userService.getMe(COMMAND)).isEmpty())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("異常系: ユーザー自身が無効な場合、例外を投げず空の Optional を返す。")
    void getMe_userInactive() {
        when(userRepository.findById(any())).thenReturn(Optional.of(userWith(1L, false, true)));

        assertThatCode(() -> assertThat(userService.getMe(COMMAND)).isEmpty())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("異常系: 所属テナントが無効な場合、例外を投げず空の Optional を返す。")
    void getMe_tenantInactive() {
        when(userRepository.findById(any())).thenReturn(Optional.of(userWith(1L, true, false)));

        assertThatCode(() -> assertThat(userService.getMe(COMMAND)).isEmpty())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("異常系: コマンドのユーザー ID が Value Object の検証に失敗する場合、例外を投げず空の Optional を返しリポジトリを呼ばない。")
    void getMe_invalidUserId() {
        assertThatCode(() -> assertThat(userService.getMe(new GetMeCommand(0L, 1L))).isEmpty())
                .doesNotThrowAnyException();

        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("異常系: コマンドのテナント ID が Value Object の検証に失敗する場合、例外を投げず空の Optional を返しリポジトリを呼ばない。")
    void getMe_invalidTenantId() {
        assertThatCode(() -> assertThat(userService.getMe(new GetMeCommand(1L, null))).isEmpty())
                .doesNotThrowAnyException();

        verify(userRepository, never()).findById(any());
    }

    // ===============================================================
    // listTenantUsers
    // ===============================================================

    // ---------------------------------------------------------------
    // listTenantUsers 正常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("正常系: リポジトリが返したユーザーが、ID 昇順の順序のまま 1:1 で TenantUserResult へ詰め替えられる。")
    void listTenantUsers_success() {
        when(userRepository.findByTenantId(new TenantId(1L))).thenReturn(List.of(
                tenantUser(1L, "admin@example.com", "ADMIN", true, true),
                tenantUser(2L, "user2@example.com", "USER", true, true),
                tenantUser(3L, "user3@example.com", "USER", false, true)
        ));

        List<TenantUserResult> result = userService.listTenantUsers(LIST_COMMAND);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(TenantUserResult::id)
                .containsExactly(1L, 2L, 3L);
        assertThat(result).extracting(TenantUserResult::email)
                .containsExactly("admin@example.com", "user2@example.com", "user3@example.com");
        assertThat(result).extracting(TenantUserResult::role)
                .containsExactly("ADMIN", "USER", "USER");
        assertThat(result).extracting(TenantUserResult::isActive)
                .containsExactly(true, true, false);
    }

    @Test
    @DisplayName("正常系: コマンドのテナント ID が TenantId に変換されてリポジトリへ渡される。")
    void listTenantUsers_convertsTenantIdToValueObject() {
        when(userRepository.findByTenantId(new TenantId(1L))).thenReturn(List.of(activeUser));

        userService.listTenantUsers(LIST_COMMAND);

        verify(userRepository).findByTenantId(new TenantId(1L));
    }

    @Test
    @DisplayName("正常系: 無効ユーザーも一覧から除外されず、isActive が false として含まれる。")
    void listTenantUsers_includesInactiveUser() {
        when(userRepository.findByTenantId(new TenantId(1L))).thenReturn(List.of(
                tenantUser(1L, "active@example.com", "USER", true, true),
                tenantUser(2L, "inactive@example.com", "USER", false, true)
        ));

        List<TenantUserResult> result = userService.listTenantUsers(LIST_COMMAND);

        assertThat(result).hasSize(2);
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).email()).isEqualTo("inactive@example.com");
        assertThat(result.get(1).isActive()).isFalse();
    }

    @Test
    @DisplayName("正常系: 所属テナントが無効なユーザーでも、isActive には users.is_active 単体の値が入る。")
    void listTenantUsers_inactiveTenantUser() {
        when(userRepository.findByTenantId(new TenantId(1L))).thenReturn(List.of(
                tenantUser(1L, "user1@example.com", "USER", true, false),
                tenantUser(2L, "user2@example.com", "USER", false, false)
        ));

        List<TenantUserResult> result = userService.listTenantUsers(LIST_COMMAND);

        assertThat(result).hasSize(2);
        // User#isActive() の AND 結果（false）ではなく userIdIsActive の値が返る
        assertThat(result.get(0).isActive()).isTrue();
        assertThat(result.get(1).isActive()).isFalse();
    }

    @Test
    @DisplayName("正常系: リポジトリが空リストを返す場合、空リストを返す。")
    void listTenantUsers_empty() {
        when(userRepository.findByTenantId(new TenantId(1L))).thenReturn(List.of());

        List<TenantUserResult> result = userService.listTenantUsers(LIST_COMMAND);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // ---------------------------------------------------------------
    // listTenantUsers 異常系
    // ---------------------------------------------------------------

    @Test
    @DisplayName("異常系: コマンドのテナント ID が 0 以下の場合、例外を投げず空リストを返しリポジトリを呼ばない。")
    void listTenantUsers_invalidTenantId() {
        assertThatCode(() -> assertThat(
                userService.listTenantUsers(new ListTenantUsersCommand(0L))).isEmpty())
                .doesNotThrowAnyException();

        verify(userRepository, never()).findByTenantId(any());
    }

    @Test
    @DisplayName("異常系: コマンドのテナント ID が null の場合、例外を投げず空リストを返しリポジトリを呼ばない。")
    void listTenantUsers_nullTenantId() {
        assertThatCode(() -> assertThat(
                userService.listTenantUsers(new ListTenantUsersCommand(null))).isEmpty())
                .doesNotThrowAnyException();

        verify(userRepository, never()).findByTenantId(any());
    }
}
