# requirements

## 作業概要
ログイン中ユーザー情報取得ユースケースの application(Service) 層本体を実装する。現在スタブ（`UnsupportedOperationException` をスロー）である `UserService.getMe(GetMeCommand)` を実装し、アクセストークン由来のユーザー ID・テナント ID から該当ユーザーを参照して、DB 由来のメールアドレスとロールを `GetMeResult` として返す。

処理方針は以下のとおり。

1. `GetMeCommand` が保持するプリミティブ（`Long userId` / `Long tenantId`）を、`getMe` の入口で Value Object（`UserId` / `TenantId`）へ変換する。VO 生成が `IllegalArgumentException` で失敗した場合は例外を伝播させず `Optional.empty()` を返す（認証フィルタ通過後のため通常発生しないが防御的に扱う）。
2. 既存の `UserRepository#findById(UserId)` で該当ユーザーを参照する。存在しなければ `Optional.empty()`。
3. **テナント越えアクセスの防止は application 層（UserService）で行う。** 取得した `User#tenantId()` が入力の `TenantId` と一致しない場合は `Optional.empty()` を返す。リポジトリ層（`UserRepository` インターフェース / `UserRepositoryImpl` / `UserJpaRepository`）は一切変更しない。
4. **無効ユーザー／無効テナントは 404 扱い。** `User#isActive()`（ユーザー自身の有効性と所属テナントの有効性の AND）が `false` の場合は `Optional.empty()` を返す。例外はスローしない。
5. 上記をすべて満たす場合のみ、`new GetMeResult(user.email().value(), user.role().value())` を `Optional` で返す。値は **DB 由来**であり、principal（アクセストークン）由来の値は使わない。

いずれの異常系でも例外をスローせず `Optional.empty()` を返す。呼び出し元の `UserController` が `orElseGet` で 404 Not Found（本文なし）に変換する。

**presentation 層（`UserController` / `UserResponse` / `GetMeCommand` / `GetMeResult`）は今回変更しない**（コミット 1d4ab62 で作成済み）。`UserController` は現状 principal の role を返すため `GetMeResult#role` は当面未使用フィールドとなるが、presentation 層の変更は本増分のスコープ外とする。

## 作業対象レイヤー
application(Service)

## 作業対象の種別
Service

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 (Jupiter) + Mockito（`@ExtendWith(MockitoExtension.class)`、アサーションは AssertJ）。既存の `src/test/java/io/github/aki0057/multitenant/auth/application/AuthServiceTest.java` の書き方・命名規則に合わせる。
- Spring MVC アノテーション: 使用しない（Service 層のため）。本体には `@Transactional(readOnly = true)`（参照専用ユースケース）、DI には Lombok の `@RequiredArgsConstructor` を使用する（既存 `AuthService` と同様）。

## 隣接レイヤー
- 1 つ外側のレイヤー: presentation(`UserController#getMe`) — 実装済み。`userService.getMe(new GetMeCommand(authenticatedUser.userId().value(), authenticatedUser.tenantId().value()))` を呼び、`Optional` が空なら 404 を返す。**今回変更しない。**
- 1 つ内側のレイヤー: domain（`UserId` / `TenantId` VO、`User` DomainObject の `tenantId()` / `isActive()` / `email()` / `role()`、`UserRepository#findById(UserId)`）— いずれも既存・実装済み。**今回変更しない。**

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
外側（`UserController#getMe`）は既に存在する。以下のシグネチャに作業対象を合わせる（変更しない）。
- メソッド名: `UserService.getMe`
- 引数: `@NonNull GetMeCommand command`。`GetMeCommand` は `record GetMeCommand(Long userId, Long tenantId)`（作成済み・完成扱い）。値はアクセストークンの `sub` / `tenantId` クレーム由来。
- 戻り値: `Optional<GetMeResult>`。`GetMeResult` は `record GetMeResult(String email, String role)`（作成済み・完成扱い）。

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド。
- メソッド名: `getMe`
- 引数: `@NonNull GetMeCommand command`
- 戻り値: `Optional<GetMeResult>`（`@Transactional(readOnly = true)` を付与）

補足: `UserService` は現在フィールドを持たないため、`private final UserRepository userRepository;` を追加し `@RequiredArgsConstructor` で DI する。スタブ実装時の `// TODO:` コメントは削除する。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
内側の部品はすべて既存であり、**新規スタブの作成は不要**（したがって TODO.md への新規スタブ登録も発生しない）。以下の既存 API を利用する。
- `new UserId(Long value)` — VO。`null` / 0 以下で `IllegalArgumentException`。
- `new TenantId(Long value)` — VO。`null` / 0 以下で `IllegalArgumentException`。
- `UserRepository.findById(UserId userId)` → `Optional<User>`（既存・実装済み。**変更しない**）
- `User.tenantId()` → `TenantId`（テナント一致判定に使用）
- `User.isActive()` → `boolean`（ユーザー有効 かつ テナント有効）
- `User.email()` → `Email`、`Email.value()` → `String`
- `User.role()` → `Role`、`Role.value()` → `String`

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
Command が保持するプリミティブ（`Long userId` / `Long tenantId`）は、`getMe` の入口で Value Object（`UserId` / `TenantId`）へ変換してから内側へ渡す。プリミティブのままドメイン層へ持ち込まない。VO 変換時の `IllegalArgumentException` は握りつぶし、`Optional.empty()` を返す。戻り値の `GetMeResult` は presentation との境界の DTO であるため、`Email` / `Role` をプリミティブ（`String`）へ変換して詰める。
- 作成する DomainObject / ValueObject 名: なし（すべて既存のものを利用。新規作成しない）
