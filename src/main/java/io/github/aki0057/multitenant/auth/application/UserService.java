package io.github.aki0057.multitenant.auth.application;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantId;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;
import io.github.aki0057.multitenant.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ユーザー情報参照のユースケースを担うサービスクラス。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * ログイン中ユーザー自身の情報を取得する。
     * アクセストークンから復元されたユーザー ID・テナント ID をもとに該当ユーザーを参照し、
     * メールアドレスとロールを返す。
     *
     * <p><strong>テナント越えアクセスの防止は本メソッド（application 層）で行う。</strong>
     * 参照したユーザーの所属テナントが入力のテナント ID と一致しない場合は、
     * ユーザーが存在しない場合と同様に {@link Optional#empty()} を返す。</p>
     *
     * <p>無効ユーザー・無効テナント（{@link User#isActive()} が {@code false}）も
     * 404 相当として扱い、例外をスローせず {@link Optional#empty()} を返す。</p>
     *
     * <p>次のいずれの場合も例外をスローせず {@link Optional#empty()} を返す。
     * 呼び出し元（presentation）はこれを 404 Not Found（本文なし）へ変換する。</p>
     * <ul>
     *   <li>ユーザー ID / テナント ID が Value Object の検証に失敗する場合
     *       （{@link IllegalArgumentException} は握りつぶす。認証フィルタ通過後のため
     *       通常は発生しないが、防御的に扱う）</li>
     *   <li>該当ユーザーが存在しない場合</li>
     *   <li>該当ユーザーの所属テナントが入力のテナント ID と一致しない場合</li>
     *   <li>ユーザー自身または所属テナントが無効な場合</li>
     * </ul>
     *
     * @param command ログイン中ユーザー情報取得コマンド
     * @return 該当ユーザーのメールアドレスとロールを含む {@link GetMeResult}。
     *         該当ユーザーが存在しない場合は {@link Optional#empty()}
     */
    @Transactional(readOnly = true)
    public Optional<GetMeResult> getMe(@NonNull GetMeCommand command) {
        final UserId userId;
        final TenantId tenantId;
        try {
            userId = new UserId(command.userId());
            tenantId = new TenantId(command.tenantId());
        } catch (IllegalArgumentException e) {
            // 認証フィルタ通過後のため通常発生しないが、防御的に 404 相当（空）へ変換する
            return Optional.empty();
        }

        return userRepository.findById(userId)
                // テナント越えアクセスの防止（application 層で判定する）
                .filter(user -> user.tenantId().equals(tenantId))
                // 無効ユーザー・無効テナントは 404 扱い
                .filter(User::isActive)
                .map(user -> new GetMeResult(user.email().value(), user.role().value()));
    }

    /**
     * 指定されたテナントに所属するユーザーの一覧を取得する。
     * 管理者が自テナントのユーザー状態を把握するためのユースケースであり、
     * 各ユーザーの ID・メールアドレス・ロール・有効状態を返す。
     *
     * <p><strong>無効ユーザー（{@code users.is_active = false}）も一覧に含める。</strong>
     * 無効ユーザー・無効テナントを 404 相当（{@link Optional#empty()}）として扱う
     * {@link #getMe(GetMeCommand)} とは意図的に方針が異なる。
     * 有効・無効の状態は {@link TenantUserResult#isActive()} で呼び出し元へ伝える。</p>
     *
     * <p><strong>テナントの有効性は判定しない。</strong>
     * {@link TenantUserResult#isActive()} へ詰めるのは {@code users.is_active} 単体
     * （{@link User#userIdIsActive()}）であり、所属テナントの有効性を含む
     * {@link User#isActive()} ではない。無効ユーザー・無効テナントのいずれによっても
     * 一覧からの除外は行わない。</p>
     *
     * <p><strong>テナント越えアクセスの防止は
     * {@link UserRepository#findByTenantId(TenantId)} のクエリ条件（テナント ID の一致）で
     * 担保する。</strong>application 層でテナント一致を判定する
     * {@link #getMe(GetMeCommand)} とはこの点でも方針が異なる。</p>
     *
     * <p>該当ユーザーが 1 件も存在しない場合は例外をスローせず空リストを返す。
     * 呼び出し元（presentation）はこれを 200 OK + 空配列へ変換する。
     * 並び順（ID 昇順）を保証する責務は
     * {@link UserRepository#findByTenantId(TenantId)} にあり、
     * 本メソッドは並び替えを行わずリポジトリが返した順序をそのまま維持する。</p>
     *
     * <p>テナント ID が Value Object の検証に失敗する場合
     * （{@link IllegalArgumentException} は握りつぶす。認証フィルタ通過後のため
     * 通常は発生しないが、防御的に扱う）も、例外をスローせず空リストを返す。</p>
     *
     * @param command 同一テナントのユーザー一覧取得コマンド
     * @return テナントに所属するユーザーを ID 昇順で並べた {@link TenantUserResult} のリスト。
     *         該当ユーザーが存在しない場合は空リスト
     */
    @Transactional(readOnly = true)
    public List<TenantUserResult> listTenantUsers(@NonNull ListTenantUsersCommand command) {
        final TenantId tenantId;
        try {
            tenantId = new TenantId(command.tenantId());
        } catch (IllegalArgumentException e) {
            // 認証フィルタ通過後のため通常発生しないが、防御的に空リストへ変換する
            return List.of();
        }

        return userRepository.findByTenantId(tenantId).stream()
                // 除外・並び替えは行わず 1:1 で詰め替える
                .map(user -> new TenantUserResult(
                        user.userId().value(),
                        user.email().value(),
                        user.role().value(),
                        user.userIdIsActive()))
                .toList();
    }
}
