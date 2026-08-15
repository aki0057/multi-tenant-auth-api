# requirements

## 作業概要
admin エンドポイント `GET /admin/users` の application(Service) 層本体を実装する。現在スタブ（`// TODO` + `List.of()` を返すだけ）の `application/UserService#listTenantUsers(ListTenantUsersCommand)` に、指定テナントに所属するユーザー一覧を返す処理を実装する。

presentation 層（`AdminController` / `TenantUserResponse`）はコミット b63971a で完成済み、`SecurityConfig` の `/admin/**` ADMIN 限定の認可設定も直前の steering `.steering/20260815-1733-config-security-admin-authorization/` で完了済みである。本 steering は残る Service 本体を実装する。

ユーザー決定事項（変更禁止）:
1. **入口で VO 変換**: `ListTenantUsersCommand` が保持する `Long tenantId` を、`listTenantUsers` の入口で `TenantId` VO へ変換する。VO 生成が `IllegalArgumentException` で失敗した場合は例外を伝播させず**空リスト**（`List.of()`）を返す（既存 `getMe` の防御的方針に合わせる。認証フィルタ通過後のため通常は発生しない）。
2. **取得手段**: **新規追加する `UserRepository#findByTenantId(TenantId tenantId)`（戻り値 `List<User>`）** で該当テナントのユーザー一覧を取得する。
3. **テナントの有効性は判定しない**: `User#tenantIdIsActive()` / `User#isActive()` によるフィルタを行わない。テナント越えアクセスは Repository のクエリ条件（tenantId 一致）で担保されるため、`getMe` のようなアプリ層でのテナント一致フィルタも不要。
4. **無効ユーザーも一覧から除外しない**: `TenantUserResult#isActive` には `User#isActive()`（user AND tenant の AND）ではなく、**`User#userIdIsActive()`（`users.is_active` 単体）** を詰める。これは `AdminController` / `TenantUserResponse` の Javadoc にある「`users.is_active = false` も一覧に含める」という記述と列の意味を一致させるための決定である。**既存 `User#isActive()` は変更しない。**
5. **並び順は Repository の責務**: ID 昇順の並び替えは Repository のクエリ側で保証する（`UserRepository#findByTenantId` の契約として Javadoc に「ID 昇順で返す」と明記する）。`UserService` 側で `sorted()` による並び替えは行わず、Repository が返した順序をそのまま維持して詰め替える。
6. **トランザクション**: `@Transactional(readOnly = true)` を付与する（既存 `getMe` と同様）。参照専用ユースケース。
7. **詰め替え**: 各 `User` を `new TenantUserResult(user.userId().value(), user.email().value(), user.role().value(), user.userIdIsActive())` へ 1:1 で詰め替え、`List<TenantUserResult>` を返す（`Optional` でラップしない。0 件は空リスト）。スタブ実装時の `// TODO` コメントは削除する。

**単一レイヤー原則**: 作業対象は application(Service) の 1 メソッドのみ。内側の infrastructure(persistence) の実クエリ実装は今回のスコープに含めない（スタブのみ作成し `TODO.md` へ積む）。

変更してはならない既存ファイル:
- presentation 層すべて（`AdminController` / `TenantUserResponse` / `UserController` / `UserResponse`）。コミット b63971a で完成済み。
- `ListTenantUsersCommand` / `TenantUserResult`（作成済み・完成扱い）
- `UserService#getMe` の実装
- `User` ドメインモデル（`isActive()` を含む）、`UserMapper`、`UserJpaRepository`、`config/SecurityConfig`

## 作業対象レイヤー
application(Service)

## 作業対象の種別
Service

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 (Jupiter) + Mockito。`docs/testing-guidelines.md` の「UseCase / Service（`@Service`）」に従い `@ExtendWith(MockitoExtension.class)`（Spring は起動しない）。`UserRepository` は `@Mock`、`UserService` は `@InjectMocks`。アサーションは AssertJ（`assertThat` / `assertThatCode`）。
  - 追記先: 既存の `src/test/java/io/github/aki0057/multitenant/auth/application/UserServiceTest.java`（新規作成ではなく追記）。既存 `getMe` のテストケース・フィクスチャ（`activeUser` / `userWith(...)`）は変更しない。
  - 結合テスト（`AdminIntegrationTest`）は Repository 実装が未完のため**今回のスコープに含めない**。
- Spring MVC アノテーション: なし（application 層のため）。使用する Spring アノテーションは既存どおり `@Service`、`@Transactional(readOnly = true)`（`org.springframework.transaction.annotation.Transactional`）、Lombok の `@RequiredArgsConstructor`。

## 隣接レイヤー
- 1 つ外側のレイヤー: presentation（`presentation/AdminController#listTenantUsers`）— 既に存在する（コミット b63971a）
- 1 つ内側のレイヤー: domain（`domain/repository/UserRepository`）— `findByTenantId` を新規追加し、その実装 `infrastructure/persistence/repository/UserRepositoryImpl#findByTenantId` はスタブとする

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる。

呼び出し元: `presentation/AdminController#listTenantUsers`

```java
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
```

呼び出される作業対象の契約:
- メソッド名: `listTenantUsers`
- 引数: `ListTenantUsersCommand command`（`record ListTenantUsersCommand(Long tenantId)`。`tenantId` はアクセストークンの `tenantId` クレーム由来）
- 戻り値: `List<TenantUserResult>`（`record TenantUserResult(Long id, String email, String role, boolean isActive)`）。`Optional` でラップしない。0 件でも `null` ではなく空リストを返す（presentation はこれを 200 OK + 空配列へ変換する）
- 並び順: ID 昇順。presentation は返ってきた順序をそのまま維持するため、順序保証の責務は application 層以降にある（本 steering では Repository の契約として下位へ委譲する）

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド。既存スタブのシグネチャを変更せず、本体のみを実装する。

- メソッド名: `listTenantUsers`（`io.github.aki0057.multitenant.auth.application.UserService`）
- 引数: `@NonNull ListTenantUsersCommand command`
- 戻り値: `List<TenantUserResult>`
- 付与するアノテーション: `@Transactional(readOnly = true)`（既存スタブに付与済みのため維持する）

処理フロー（実装方針）:
1. `try { tenantId = new TenantId(command.tenantId()); } catch (IllegalArgumentException e) { return List.of(); }`
   - `TenantId` は `null` / 0 以下で `IllegalArgumentException` をスローする（`domain/model/vo/TenantId` の compact constructor）。認証フィルタ通過後のため通常は発生しないが、`getMe` と同様に防御的に空リストへ変換する。
2. `userRepository.findByTenantId(tenantId)` でユーザー一覧を取得する。
3. `stream().map(...)` で 1:1 に詰め替える。**フィルタ（`filter`）・並び替え（`sorted`）は行わない。**
   - `new TenantUserResult(user.userId().value(), user.email().value(), user.role().value(), user.userIdIsActive())`
4. `.toList()` で `List<TenantUserResult>` を返す。
5. スタブ実装時の `// TODO` コメントを削除する。

### 期待する挙動（受け入れ条件）
- Repository が返した順序（ID 昇順）が維持され、件数と要素が 1:1 で `TenantUserResult` へ詰め替えられる。
- `userIdIsActive = false` のユーザーも一覧から除外されず、`TenantUserResult#isActive` に `false` が入る。
- `tenantIdIsActive = false`（テナント無効）のユーザーでも `TenantUserResult#isActive` は `users.is_active` 単体（`userIdIsActive`）の値になる。テナント有効性による除外・値の変化は起きない。
- Repository が空リストを返した場合、空リストをそのまま返す（`null` を返さない・例外を投げない）。
- `command.tenantId()` が `null` または 0 以下の場合、例外をスローせず空リストを返し、`UserRepository` を呼ばない。
- `getMe` の既存挙動は一切変わらない。

### Javadoc 方針
- 既存スタブの Javadoc は概ね妥当なため活かしつつ、次を明記・修正する。
  - `isActive` に詰めるのは `users.is_active` 単体（`User#userIdIsActive()`）であり、テナントの有効性を含む `User#isActive()` ではないこと。
  - テナントの有効性は判定せず、無効テナント・無効ユーザーのいずれも一覧から除外しないこと（`getMe` と意図的に方針が異なる）。
  - テナント越えアクセスの防止は Repository のクエリ条件（tenantId 一致）で担保すること（`getMe` は application 層で判定するのに対し、本メソッドは判定しない）。
  - 並び順（ID 昇順）の保証責務は `UserRepository#findByTenantId` にあり、本メソッドは順序をそのまま維持すること。
  - `tenantId` が Value Object の検証に失敗した場合は例外をスローせず空リストを返すこと。
  - 既存 Javadoc 内の「無効ユーザー・無効テナントを 404 相当（`Optional#empty()`）として扱う `getMe` とは意図的に方針が異なる」旨の記述は残す。

### テスト方針
`src/test/java/io/github/aki0057/multitenant/auth/application/UserServiceTest.java` へ `listTenantUsers` のテストを追記する。既存 `getMe` のセクション区切りコメント（`// ===` / `// ---`）の書式に倣い、`listTenantUsers` 用のセクションを追加する。命名は `methodName_condition()`、`@DisplayName` は日本語で先頭に `正常系:` / `異常系:` を付ける（`docs/testing-guidelines.md`）。

正常系:
- `listTenantUsers_success()` — 複数件が Repository の返した順序（ID 昇順）のまま `TenantUserResult` へ 1:1 で詰め替えられること（件数・`id` / `email` / `role` / `isActive` の各値・順序を検証）。
- `listTenantUsers_convertsTenantIdToValueObject()` — コマンドの `tenantId` が `TenantId` に変換されて `findByTenantId` へ渡されること（`verify(userRepository).findByTenantId(new TenantId(1L))`）。
- `listTenantUsers_includesInactiveUser()` — `userIdIsActive = false` のユーザーが除外されず、`isActive = false` として含まれること。
- `listTenantUsers_inactiveTenantUser()` — `tenantIdIsActive = false` のユーザーでも `isActive` は `users.is_active` 単体（`userIdIsActive`）の値が返ること（`User#isActive()` の AND 結果ではないこと）。
- `listTenantUsers_empty()` — Repository が空リストを返す場合、空リストを返すこと。

異常系:
- `listTenantUsers_invalidTenantId()` — `tenantId` が 0 以下（例: `0L`）で `TenantId` の生成に失敗する場合、例外をスローせず空リストを返し、`verify(userRepository, never()).findByTenantId(any())` を検証すること。
- `listTenantUsers_nullTenantId()` — `tenantId` が `null` の場合も同様に、例外をスローせず空リストを返し `UserRepository` が呼ばれないこと。

補足:
- 既存フィクスチャ `userWith(long tenantId, boolean userIdIsActive, boolean tenantIdIsActive)` は `userId` を差し替えられないため、一覧の順序・複数件を検証するための補助メソッド（例: `tenantUser(long userId, String email, String role, boolean userIdIsActive, boolean tenantIdIsActive)`）を追加してよい（Javadoc を付ける）。既存の `activeUser` / `userWith` および `getMe` のテストケースは変更しない。
- 結合テスト（`AdminIntegrationTest`）は `UserRepositoryImpl#findByTenantId` がスタブのため今回作成しない（後続 steering で実施する）。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
内側はまだ存在しないため、スタブとして新規作成する（実装しない・TODO のみ）。

1. `domain/repository/UserRepository` へメソッドを**追加**する（インターフェースのため実装は持たない）。
   - メソッド名: `findByTenantId`
   - 引数: `TenantId tenantId`
   - 戻り値: `List<User>`
   - Javadoc に次を明記する（Repository としての契約）:
     - 指定テナントに所属するユーザーを**すべて**返すこと（無効ユーザーも除外しない）
     - **ID 昇順で返すこと**（並び順の保証は本メソッドの責務）
     - 該当なしの場合は `null` ではなく**空リスト**を返すこと
   - 既存の `findByTenantCodeAndEmail` / `findById` は変更しない。`java.util.List` の import を追加する。

2. `infrastructure/persistence/repository/UserRepositoryImpl#findByTenantId` を**スタブ**として作成する。
   - `@Override public List<User> findByTenantId(TenantId tenantId)`
   - 中身は実装せず `// TODO` を記載して `List.of()` を返すのみとする（実際の JPA クエリ実装は後続 steering）。
   - 既存の `findByTenantCodeAndEmail` / `findById` は変更しない。

3. `infrastructure/persistence/repository/UserJpaRepository` は**今回変更しない**。後続 steering で実クエリと `@EntityGraph(attributePaths = "tenant")` による N+1 対策を実施する。

`TODO.md` への登録:
- 「## Repository (domain)」の章へ `findByTenantId（UserRepository）` を空チェックボックスで追記する。
- 「## infrastructure.persistence」の章へ `UserRepositoryImpl#findByTenantId を実装` を空チェックボックスで追記する。
- 「## Service (application)」の章の既存項目 `- [ ] UserService#listTenantUsers(ListTenantUsersCommand)` のチェックを埋める。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
プリミティブ型への依存を禁止する。`ListTenantUsersCommand` が保持するプリミティブ（`Long tenantId`）は、`listTenantUsers` の入口で Value Object へ変換し、プリミティブのままドメイン層へ持ち込まない。

- 作成する DomainObject / ValueObject 名: **なし（新規作成は発生しない）**
  - 利用する既存 ValueObject: `domain/model/vo/TenantId`（`record TenantId(Long value)`。`null` / 0 以下で `IllegalArgumentException`）
  - 利用する既存 DomainObject: `domain/model/User`（`userId()` / `email()` / `role()` / `userIdIsActive()` を参照する。**`User` は変更しない**。とくに `isActive()` の定義は変更しない）
  - `TenantUserResult` への詰め替え時のみ、境界の出力 DTO であるため VO から `value()` でプリミティブを取り出す（`user.userId().value()` / `user.email().value()` / `user.role().value()`）。
