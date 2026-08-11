package io.github.aki0057.multitenant.auth.application;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * ユーザー情報参照のユースケースを担うサービスクラス。
 */
@Service
public class UserService {

    /**
     * ログイン中ユーザー自身の情報を取得する。
     * アクセストークンから復元されたユーザー ID・テナント ID をもとに該当ユーザーを参照し、
     * メールアドレスとロールを返す。
     *
     * @param command ログイン中ユーザー情報取得コマンド
     * @return 該当ユーザーのメールアドレスとロールを含む {@link GetMeResult}。
     *         該当ユーザーが存在しない場合は {@link Optional#empty()}
     */
    // TODO: ユーザー ID / テナント ID による該当ユーザーの参照を実装（後続 application 増分）
    public Optional<GetMeResult> getMe(@NonNull GetMeCommand command) {
        throw new UnsupportedOperationException("UserService#getMe は未実装です");
    }
}
