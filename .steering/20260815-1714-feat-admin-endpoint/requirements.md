# requirements

## 作業概要
ロールが ADMIN のユーザーが、自分と同一テナントに所属するユーザーの一覧を取得するエンドポイント `GET /admin/users` を presentation 層に新規作成する。既存の `GET /users/me`（`presentation/UserController#getMe`）の「一覧版」にあたる。

`presentation/AdminController` を新規作成する（既存 `UserController` には追加しない）。テナント ID は URL パラメータではなく、`presentation/filter/JwtAuthenticationFilter` が SecurityContext にセットする `domain.model.AuthenticatedUser`（`UserId userId` / `TenantId tenantId` / `Role role`）から取得する。Controller は `@AuthenticationPrincipal AuthenticatedUser` で受け取り、`authenticatedUser.tenantId().value()` を内側の Command へ詰める（`UserController#getMe` と同じ方式）。

レスポンスのトップレベルはラップせず素の配列とし、要素の DTO は新規作成する `presentation/TenantUserResponse`（record・`id` / `email` / `role` / `isActive` の 4 フィールド）とする。既存 `presentation/UserResponse` は再利用しない（フィールド構成が異なるため）。

内側は既存 `application/UserService` に**メソッドを追加**する形とし、新規 Service クラスは作らない。今回は presentation 層の作業のため、追加メソッド `UserService#listTenantUsers` はスタブ（`// TODO` のみ・中身を実装しない）として作成し `TODO.md` に登録する。入力 DTO の `ListTenantUsersCommand` および出力 DTO の `TenantUserResult` は作成と同時に完成扱いとし `TODO.md` には登録しない。

**認可設定（`config/SecurityConfig` への `requestMatchers("/admin/**").hasAuthority("ADMIN")` 追加）は本 steering のスコープ外**であり、種別 `config` の別 steering で後続実施する。したがって本作業では `config/SecurityConfig` を一切変更しない。また ADMIN 判定を SecurityConfig 側へ寄せる方針のため、Controller に `@PreAuthorize` 等の認可アノテーションは付けない。

前提事実（既存挙動・変更しない）:
- `JwtAuthenticationFilter` は `new SimpleGrantedAuthority(role.value())`（= `"ADMIN"` / `"USER"`。`ROLE_` 接頭辞なし）を authority としてセットする。よって認可設定側は `hasRole` ではなく `hasAuthority("ADMIN")` を用いる。
- USER ロールでアクセスした場合は、既存 `defaultSecurityFilterChain` の `accessDeniedHandler` により 403 ではなく 404 Not Found（`{"error": "Not Found"}`）が返る（既存挙動を踏襲する）。ただしこの挙動は SecurityConfig の認可設定が入って初めて発生するため、本 steering のテスト範囲には含めない（config の別 steering で検証する）。

## 作業対象レイヤー
presentation (API / Controller)

## 作業対象の種別
API

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 + Mockito。Controller テストは `@WebMvcTest(AdminController.class)` + `@Import({SecurityConfig.class, PasswordEncoderConfig.class, GlobalExceptionHandler.class})`、`UserService` は `@MockitoBean`、`SecurityConfig#defaultSecurityFilterChain` が要求する `AccessTokenVerifier` も `@MockitoBean` で満たす。認証済み principal は `SecurityMockMvcRequestPostProcessors.authentication(...)` PostProcessor で `AuthenticatedUser` を principal にセットして再現する（既存 `src/test/java/.../presentation/UserControllerTest.java` の書き方を踏襲）。アサーションは AssertJ（`jsonPath` によるレスポンス検証と併用）。
- Spring MVC アノテーション: `@RestController`、`@GetMapping("/admin/users")`、`@AuthenticationPrincipal`、Swagger の `@Operation` / `@SecurityRequirement(name = "bearerAuth")`（`UserController#getMe` に倣う）、DTO の `@Schema`。

## 隣接レイヤー
- 1 つ外側のレイヤー: なし（API が最も外側で、呼び出し元は外部クライアント）
- 1 つ内側のレイヤー: application（既存 `application/UserService` へメソッド追加）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
該当なし（作業対象が API のため）。

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド。
- メソッド名: `listTenantUsers`（クラス: `presentation/AdminController`、新規作成）
- 引数: `@AuthenticationPrincipal AuthenticatedUser authenticatedUser`
- 戻り値: `ResponseEntity<List<TenantUserResponse>>`（常に 200 OK。0 件の場合も 404 ではなく 200 + 空配列 `[]`）

補足（ユーザー決定事項・レスポンス仕様。変更禁止）:
- パス／メソッド: `GET /admin/users`。既存 `UserController` には追加せず `AdminController` を新規作成する。
- テナント ID の取得元: URL パラメータではなく `@AuthenticationPrincipal AuthenticatedUser`（`UserController#getMe` と同じ方式）。
- レスポンス形状: トップレベルは素の配列（ラップしない）。`ResponseEntity.ok(List<TenantUserResponse>)` を返す。
- レスポンス要素 DTO: `presentation/TenantUserResponse`（新規作成・record）。フィールドは `Long id` / `String email` / `String role` / `boolean isActive` の 4 つ。既存 `UserResponse` は再利用しない。Controller は内側から返る `TenantUserResult` を 1:1 で `TenantUserResponse` へ詰め替える。
- 管理者本人も一覧に含める（除外しない）。
- 無効ユーザー（`users.is_active = false`）も一覧に**含める**。状態は `isActive` フィールドで返す。
  - **既存 `UserService#getMe` との方針差異（明記）**: `getMe` は無効ユーザー・無効テナントを 404 相当（`Optional.empty()`）として扱うが、本エンドポイントは管理者が自テナントのユーザー状態を把握する用途のため、**意図的に方針が異なり無効ユーザーも返す**。この差異は仕様であり、`getMe` 側の挙動は変更しない。
- 並び順: `id` 昇順。並び替えの責務は内側（`UserService#listTenantUsers` 以降）にあり、Controller は返ってきた順序をそのまま維持する。
- 0 件: 404 ではなく 200 OK + 空配列。
- ページング・検索フィルタ: 今回のスコープに含めない。
- 認可: Controller に `@PreAuthorize` 等を付けない。ADMIN 判定は `config/SecurityConfig` の `requestMatchers("/admin/**").hasAuthority("ADMIN")` に寄せる（別 steering）。本作業で `config/SecurityConfig` は変更しない。
- Swagger: `@Operation`（summary / description）と `@SecurityRequirement(name = "bearerAuth")` を `UserController#getMe` に倣って付与する。
- 異常系(1) 未認証・不正／期限切れトークン: 既存 `authenticationEntryPoint` により 401 Unauthorized（`{"error": "Unauthorized"}`）。Controller には到達しない。
- 異常系(2) USER ロールでのアクセス: 既存 `accessDeniedHandler` により 403 ではなく 404 Not Found。ただし SecurityConfig への認可設定追加が前提のため、本 steering では実装・テストとも対象外。
- `GlobalExceptionHandler`・`AuthenticatedUser`・`JwtAuthenticationFilter`・`UserController`・`UserResponse`・`SecurityConfig` は変更しない。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
内側はまだ存在しないため、スタブとして新規作成する（実装しない・TODO のみ）。
- メソッド名: `UserService#listTenantUsers`（クラス: 既存 `application/UserService` へ**メソッド追加**。新規 Service クラスは作らない）
- 引数: `ListTenantUsersCommand`（入力 DTO の Command。`application/`、record。保持するのは `Long tenantId` のみ。認可は SecurityConfig 側へ寄せるため `role` / `userId` は運ばない）
- 戻り値: `List<TenantUserResult>`（`Optional` でラップしない。0 件は空リスト）
- 補足: `TenantUserResult` は Service の出力オブジェクト（`application/`、record）。既存 `GetMeResult` に倣いプリミティブ型を保持する。フィールドは `Long id` / `String email` / `String role` / `boolean isActive`。
- 補足: 追加する `UserService#listTenantUsers` はスタブとし、メソッドに `// TODO` を記載して中身を実装しない（同一テナントのユーザー検索・id 昇順ソート・無効ユーザーを含める判断・プリミティブ → ValueObject 変換はすべて後続タスクで実装する）。既存 `UserService#getMe` の実装には手を加えない。
- Command の扱い: `ListTenantUsersCommand` はこのステップで作成するが、スタブ（実装しない・TODO のみ）ではなく作成と同時に完成扱いとし、`TODO.md` には登録しない。詳細は `docs/file-change-workflow.md` の「Command（Service の入力 DTO）の扱い」を参照。なお `TenantUserResult` は Service の出力 DTO（ロジックを持たない record）であり、Command と同様に作成と同時に完成扱いとし `TODO.md` には登録しない（既存 `LoginResult` / `RefreshResult` / `GetMeResult` と同じ扱い）。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
該当なし（作業対象が API のため）。プリミティブ → ValueObject（`TenantId` 等）への変換は、後続タスクで内側の `UserService#listTenantUsers` の入口に実装する。
