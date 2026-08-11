package io.github.aki0057.multitenant.auth.application;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantId;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;
import io.github.aki0057.multitenant.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
