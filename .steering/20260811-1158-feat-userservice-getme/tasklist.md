# tasklist

- [x] 作業対象を実装する（`UserService#getMe(GetMeCommand)` の本体。`UserRepository` を `@RequiredArgsConstructor` で DI し `@Transactional(readOnly = true)` を付与。入口で `UserId` / `TenantId` へ変換し、`findById` → テナント ID 一致判定 → `isActive()` 判定を通過した場合のみ `new GetMeResult(user.email().value(), user.role().value())` を返す。VO 生成失敗・ユーザー不在・テナント不一致・無効ユーザー／無効テナントはいずれも例外を投げず `Optional.empty()` を返す。スタブ実装時の `// TODO:` コメントを削除する。presentation 層とリポジトリ層は変更しない）
- [x] Javadoc を記載する（既存スタブの Javadoc を整え、テナント越えアクセス防止を application 層で行うこと・無効ユーザー／無効テナントは `Optional.empty()`（404 相当）で例外を投げないこと・返す email / role が DB 由来であることを明記する）
- [x] 正常系のテストコードを記載する（`src/test/java/io/github/aki0057/multitenant/auth/application/UserServiceTest.java` を新規作成。`@ExtendWith(MockitoExtension.class)` + `@Mock UserRepository` + `@InjectMocks UserService`。有効ユーザーかつテナント ID 一致のとき、DB 由来の email / role を持つ `GetMeResult` が返ることを検証する）
- [x] 異常系のテストコードを記載する（ユーザー不在（`findById` が空）／テナント ID 不一致（他テナントのユーザー）／ユーザー無効（`userIdIsActive` が false）／テナント無効（`tenantIdIsActive` が false）の各ケースで、例外を投げず `Optional.empty()` が返ることを検証する。命名は `getMe_userNotFound()` のように `methodName_condition()` とし、`@DisplayName` は日本語で先頭に `正常系:` / `異常系:` を付ける）
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックスを埋める（`## Service (application)` の `- [ ] UserService#getMe(GetMeCommand)` を完了にする）
