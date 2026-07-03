# requirements

## 作業概要
`AuthController` に `POST /refresh` エンドポイントを追加する。このエンドポイントが用いる入力 DTO `RefreshRequest`（新規）と出力 DTO `RefreshResponse`（新規）を作成する。呼び出し先である `application` 層はまだ存在しないため、`AuthService#refresh` をスタブとして新規作成し、その入力 Command である `RefreshCommand`（新規・完成扱い）と、スタブの戻り値を表す `RefreshResult`（新規・ロジックを持たない record）を合わせて作成する。

あわせて、新規 API `/refresh` を認証不要にするための authorization 結線として、`SecurityConfig`（`config/`）の `authorizeHttpRequests` の permitAll に `/refresh` を追加する。`SecurityConfig` は config 層のクラスであり主たる作業対象レイヤー（presentation(API)）そのものではないが、既存の `/login` も同じ `authorizeHttpRequests` の permitAll に登録済みという前例に倣い、新規追加する `/refresh` API を外部公開するために必要な認可設定として本タスクのスコープに含める。変更は最小限（`.requestMatchers("/login", "/refresh").permitAll()` の 1 行）にとどめ、`SecurityConfig` のその他の設定（JWT フィルター挿入・例外ハンドリング等）には手を入れない。

本タスクのスコープは presentation 層（API）を主対象とする。次は今回のスコープに含めない。
- `POST /login`（`AuthController#login` / `LoginResponse` / `AuthService#login`）への変更
- `AuthService#login` のトランザクション境界（`@Transactional(readOnly = true)`）の変更
- `AuthService#refresh` の実処理（トークン検証・ローテーション・永続化）、および domain / infrastructure 層のクラス（`RefreshToken`、`RefreshTokenRepository` 等）
- `SecurityConfig` の permitAll 追加以外の変更（JWT フィルター・例外ハンドリング等の見直し）

## 作業対象レイヤー
presentation (API)

（補足: `SecurityConfig` の permitAll 追加は config 層への最小限の付随変更であり、`/refresh` API を認証不要として公開するために必須の結線作業のため、主レイヤーを presentation(API) としたまま本タスクに含める。）

## 作業対象の種別
API

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 (Jupiter) + AssertJ + Mockito（`@MockitoBean` で `AuthService` をモック）
- Spring MVC アノテーション: `@WebMvcTest(AuthController.class)` + `@Import({SecurityConfig.class, PasswordEncoderConfig.class, GlobalExceptionHandler.class})`、`MockMvc` で `POST /refresh` を検証する。
  既存の `AuthControllerTest`（`src/test/java/.../presentation/AuthControllerTest.java`）に `refresh_*` テストメソッドを追記する形とする（新規テストクラスは作成しない）。`SecurityConfig` を `@Import` しているため、permitAll 追加により `refresh_success` 等が 401 にならず本来のステータスで検証できる。

## 隣接レイヤー
- 1 つ外側のレイヤー: なし（`AuthController` は HTTP 呼び出しを直接受ける最も外側のレイヤー）
- 1 つ内側のレイヤー: application (`AuthService`)

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド。
- メソッド名: `AuthController#refresh`
- 引数: `@Valid @RequestBody RefreshRequest request`
- 戻り値: `ResponseEntity<RefreshResponse>`

併せて作成する DTO:
- `RefreshRequest`（`presentation/`）— 既存 `LoginRequest` の慣習（`@Getter @NoArgsConstructor` ＋ Bean Validation 注釈）に倣う。
  - フィールド: `private String refreshToken;`（`@NotBlank(message = "refreshToken は必須です")`）
- `RefreshResponse`（`presentation/`）— 既存 `LoginResponse` の慣習（record）に倣う。
  - シグネチャ: `public record RefreshResponse(String accessToken, String refreshToken, String tokenType) {}`
  - `tokenType` は `"Bearer"` を固定で返す（`login` と同様）。

併せて変更する config:
- `SecurityConfig`（`config/`）— `authorizeHttpRequests` の permitAll に `/refresh` を追加する。
  - 変更前: `.requestMatchers("/login").permitAll()`
  - 変更後: `.requestMatchers("/login", "/refresh").permitAll()`
  - この変更は設定値の追加のみであり、スタブでも DomainObject でもないため `TODO.md` への登録は不要。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
内側（application 層）はまだ存在しないため、`AuthService#refresh` をスタブとして新規作成する（実装しない・`// TODO` のみ）。
- メソッド名: `AuthService#refresh`
- 引数: `RefreshCommand command`
- 戻り値: `RefreshResult`
- 補足（Command）: `RefreshCommand` は本ステップで新規作成する application 層の入力 DTO（record）。既存 `LoginCommand` と同様に、`AuthController#refresh` が `new RefreshCommand(request.getRefreshToken())` を呼ぶために必要。ロジックを持たない record であり、作成と同時に完成扱いとし `TODO.md` には登録しない。
  - シグネチャ: `public record RefreshCommand(String refreshToken) {}`
- 補足（戻り値 `RefreshResult`）: `AuthService#refresh` の戻り値としてアクセストークンと新リフレッシュトークンの 2 値を返す必要があるため、本ステップで application 層に新規作成するプレーンな出力 record。`RefreshCommand` と同様にロジックを持たないため、作成と同時に完成扱いとし `TODO.md` には登録しない（`AuthService#refresh` 本体のみ `TODO.md` に登録する）。
  - シグネチャ: `public record RefreshResult(String accessToken, String refreshToken) {}`
- `AuthController#refresh` からの呼び出しイメージ:
  ```java
  RefreshResult result = authService.refresh(new RefreshCommand(request.getRefreshToken()));
  return ResponseEntity.ok(new RefreshResponse(result.accessToken(), result.refreshToken(), "Bearer"));
  ```
