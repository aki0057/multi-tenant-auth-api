# requirements

## 作業概要
ユーザー自身の情報を返すエンドポイント `GET /users/me` を presentation 層に新規作成する。`presentation/UserController` を新規作成し（既存 `AuthController` には追加しない）、ログイン済み（有効なアクセストークンを持つ）ユーザー自身の role とメールアドレスを JSON で返す。

principal は `presentation/filter/JwtAuthenticationFilter` が SecurityContext にセットする `domain.model.AuthenticatedUser`（`UserId userId` / `TenantId tenantId` / `Role role` を保持。email は持たない）であり、Controller は `@AuthenticationPrincipal` で受け取る。

JWT に email クレームは無いため、email は内側の application 層（新規 `UserService`）が userId（＋ tenantId）で DB を参照して取得する方式とする。JWT クレームおよび `AuthenticatedUser` の変更は行わない。今回は presentation 層の作業のため、`UserService` はスタブ（`// TODO` のみ・中身を実装しない）として作成し `TODO.md` に登録する。入力 DTO の Command は作成と同時に完成扱いとし `TODO.md` には登録しない。

認証は既存 `config/SecurityConfig#defaultSecurityFilterChain` の `anyRequest().authenticated()` により保護済みのため、SecurityConfig の変更は行わない（本作業の対象に含めない）。未認証・不正トークンは既存の `authenticationEntryPoint` により 401 が返る。認証済みだが該当ユーザーが DB に存在しない場合は 404 Not Found を返す。

## 作業対象レイヤー
presentation (API / Controller)

## 作業対象の種別
API

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 + Mockito（Controller テストは `@WebMvcTest(UserController.class)` + `@Import({SecurityConfig.class, PasswordEncoderConfig.class, GlobalExceptionHandler.class})`、`UserService` は `@MockitoBean`、`SecurityConfig` が要求する `AccessTokenVerifier` も `@MockitoBean` で満たす。認証済み principal は `SecurityMockMvcRequestPostProcessors.authentication(...)` 等で `AuthenticatedUser` を principal にセットして再現する）。アサーションは AssertJ。
- Spring MVC アノテーション: `@RestController`、`@GetMapping("/users/me")`、`@AuthenticationPrincipal`、`@Operation` / `@SecurityRequirement` 等の Swagger アノテーション（既存 `AuthController` に準拠）

## 隣接レイヤー
- 1 つ外側のレイヤー: なし（API が最も外側で、呼び出し元は外部クライアント）
- 1 つ内側のレイヤー: application（新規 `UserService`）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
該当なし（作業対象が API のため）。

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド。
- メソッド名: `getMe`（クラス: `presentation/UserController`、新規作成）
- 引数: `@AuthenticationPrincipal AuthenticatedUser authenticatedUser`
- 戻り値: `ResponseEntity<UserResponse>`（200 OK。該当ユーザーが存在しない場合は 404 Not Found／本文なし）

補足（ユーザー決定事項・レスポンス仕様）:
- パス／メソッド: `GET /users/me`。`AuthController` には追加せず `UserController` を新規作成する。
- レスポンス DTO: `presentation/UserResponse`（新規作成、record）。フィールドは `String email` と `String role`。`AuthenticatedUser` の `Role`（VO）はプリミティブ `String` へ変換して返す（email は内側から返る値をそのまま載せる）。
- principal: `JwtAuthenticationFilter` が SecurityContext にセットする `domain.model.AuthenticatedUser`（`userId` / `tenantId` / `role`）。email を持たないため email は内側から取得する。`AuthenticatedUser` および JWT クレームは変更しない。
- 認可: 既存 `defaultSecurityFilterChain` の `anyRequest().authenticated()` で保護済み。`config/SecurityConfig` は変更しない（本 steering の対象外）。
- 異常系(1) 未認証・不正／期限切れトークン: 既存 `authenticationEntryPoint` により 401 Unauthorized（`{"error": "Unauthorized"}`）。Controller には到達しない。
- 異常系(2) 認証済みだが該当ユーザーが DB に存在しない: 内側 `UserService#getMe` が空（`Optional.empty()`）を返し、Controller が `ResponseEntity.notFound().build()` で 404 Not Found を返す。`GlobalExceptionHandler` は変更しない（新規のドメイン例外を作らず presentation 層内で完結させるため）。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
内側はまだ存在しないため、スタブとして新規作成する（実装しない・TODO のみ）。
- メソッド名: `UserService#getMe`（クラス: `application/UserService`、新規作成・スタブ。既存 `AuthService` には追加しない）
- 引数: `GetMeCommand`（入力 DTO の Command。プリミティブ `Long userId` と `Long tenantId` を保持する record）
- 戻り値: `Optional<GetMeResult>`（`GetMeResult` は `String email` と `String role` を保持する record。該当ユーザーが存在しない場合は `Optional.empty()`）
- 補足: Controller は `authenticatedUser.userId().value()` と `authenticatedUser.tenantId().value()` から `GetMeCommand` を生成して渡す。プリミティブ → ValueObject（`UserId` / `TenantId`）への変換および DB 参照（email 取得）は、後続タスクで `UserService#getMe` の入口以降に実装する。本作業では `UserService` はクラスに `// TODO` を記載したスタブとし、中身を実装しない。
- Command の扱い: `GetMeCommand` はこのステップで作成するが、スタブ（実装しない・TODO のみ）ではなく作成と同時に完成扱いとし、`TODO.md` には登録しない。詳細は `docs/file-change-workflow.md` の「Command（Service の入力 DTO）の扱い」を参照。なお `GetMeResult` は Service の出力 DTO（ロジックを持たない record）であり、Command と同様に作成と同時に完成扱いとし `TODO.md` には登録しない（既存 `LoginResult` / `RefreshResult` と同じ扱い）。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
該当なし（作業対象が API のため）。プリミティブ → ValueObject 変換は内側の `UserService#getMe` の入口で行う。
