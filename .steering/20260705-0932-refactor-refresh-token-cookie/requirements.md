# requirements

## 作業概要

リフレッシュトークンの受け渡しを JSON ボディから HttpOnly Cookie 方式へ移行する（presentation 層増分）。

- `POST /login`: 成功時、リフレッシュトークンをレスポンス JSON ボディから削除し、`Set-Cookie` ヘッダで返す。アクセストークンと `tokenType` は従来どおり JSON ボディで返す。あわせて、後続の `POST /refresh` が CSRF トークンを所持できるよう、login レスポンス時点で `XSRF-TOKEN` クッキーが発行される構成にする（鶏卵問題の回避）。
- `POST /refresh`: リクエスト JSON ボディ（`RefreshRequest`）ではなく Cookie からリフレッシュトークンを受け取る。成功時、ローテーション後の新しいリフレッシュトークンを `Set-Cookie` ヘッダで返し、JSON ボディからは削除する。
- CSRF: `/refresh` のみ CSRF 保護を有効化し、それ以外は従来どおり無効。SecurityConfig の SecurityFilterChain を `/refresh` とそれ以外で分割して対応する。
- ゴール: Swagger UI で login → refresh の動作確認ができること。CORS・非ブラウザクライアント対応は考慮外。

**リフレッシュ用 Cookie の属性**: `HttpOnly; Secure; SameSite=Strict; Path=/refresh`（Cookie 名は `refreshToken`）。`ResponseCookie`（SameSite 指定可）で付与する。

## 作業対象レイヤー

presentation（API / Controller）。ただし本エンドポイント群を成立させるための Spring フレームワーク設定として、`config/SecurityConfig`（SecurityFilterChain 分割・CSRF）と `application.properties`（springdoc の CSRF 連携）の付随変更を含む。ドメイン/アプリケーション層のロジックは対象外。

## 作業対象の種別

API（presentation）

## 使用するテスト・フレームワーク等

- テストフレームワーク: JUnit（`@WebMvcTest` + MockMvc、`AuthService` / `AccessTokenVerifier` は `@MockitoBean`）
- Spring MVC アノテーション: `@RestController` / `@PostMapping` / `@RequestBody`（login のみ継続）/ `@CookieValue`（refresh の入力）/ `@Valid`
- Spring Security: 複数 `SecurityFilterChain`、`securityMatcher`、`CookieCsrfTokenRepository.withHttpOnlyFalse()`、`ResponseCookie`

## 隣接レイヤー

- 1 つ外側のレイヤー: なし（API が最外殻。呼び出し元は Swagger UI / ブラウザ）
- 1 つ内側のレイヤー: application(AuthService) — **既存・変更禁止**

## 作業対象メソッドのシグネチャ

作業対象は既存の `AuthController` の 2 メソッド（API 種別のためコントローラ全体を単位とする）。

### AuthController#login
- メソッド名: `login`
- 引数: `@Valid @RequestBody LoginRequest request`（Cookie / CsrfToken 等の書き出しに必要な追加引数は実装時に選択してよいが、`LoginRequest` の契約は変更しない）
- 戻り値: `ResponseEntity<LoginResponse>`（200 OK）。ボディは `LoginResponse(accessToken, tokenType)`。加えて次の 2 つの `Set-Cookie` を付与する。
  - リフレッシュトークン: `refreshToken=<raw>; HttpOnly; Secure; SameSite=Strict; Path=/refresh`
  - CSRF: `XSRF-TOKEN`（`HttpOnly` でない。`/refresh` の初回 POST で送信できるよう login レスポンス時点で確実に書き出す）

### AuthController#refresh
- メソッド名: `refresh`
- 引数: `@CookieValue("refreshToken") String refreshToken`（旧 `@RequestBody RefreshRequest` を廃止）
- 戻り値: `ResponseEntity<RefreshResponse>`（200 OK）。ボディは `RefreshResponse(accessToken, tokenType)`。加えてローテーション後の新リフレッシュトークンを上記と同一属性の `Set-Cookie`（`refreshToken`）で付与する。

## 内側レイヤーへの契約

本作業は既存エンドポイントの入出力トランスポートの変更であり、内側の application 層（`AuthService`）は既に存在し、契約（生トークン文字列の受け渡しインターフェース）は不変である。したがって**新規スタブは作成しない**（TODO.md への内側スタブ登録も発生しない）。呼び出す既存メソッドは以下。

- `AuthService#login(LoginCommand)` → `LoginResult(accessToken, refreshToken)`（既存）
  - `LoginCommand(tenantCode, email, password)`（既存 record）
- `AuthService#refresh(RefreshCommand)` → `RefreshResult(accessToken, refreshToken)`（既存）
  - `RefreshCommand(String refreshToken)`（既存 record。`@CookieValue` で得た生トークン文字列を渡す）

## 付随して変更する presentation / config / resources

- `presentation/LoginResponse`: `refreshToken` フィールドを削除し `record LoginResponse(String accessToken, String tokenType)` にする。
- `presentation/RefreshResponse`: `refreshToken` フィールドを削除し `record RefreshResponse(String accessToken, String tokenType)` にする。
- `presentation/RefreshRequest`: Cookie 方式移行により未使用となるため削除する。
- `presentation/advice/GlobalExceptionHandler`: Cookie 未送付時（`@CookieValue` 欠落 → `MissingRequestCookieException`）を 401 相当で返すハンドラを追加する（500 にしない）。
- `config/SecurityConfig`: 単一 `SecurityFilterChain` を 2 本に分割する。
  - `/refresh` 用チェーン（`securityMatcher("/refresh")`、高優先）: CSRF 有効。`CookieCsrfTokenRepository.withHttpOnlyFalse()`（`XSRF-TOKEN` クッキー方式）を使う。STATELESS。permitAll。Spring Security 6 の遅延（deferred）生成に注意し、`/refresh` チェーン単独で発行すると初回 POST が 403 になるため、login レスポンス時点で `XSRF-TOKEN` を発行できる構成にする。
  - デフォルトチェーン（それ以外）: 従来どおり CSRF 無効・STATELESS・`/login` `/swagger-ui/**` `/v3/api-docs/**` permitAll・JWT フィルター挿入・例外ハンドリング（401/404）を維持する。
- `application.properties`: Swagger UI が `X-XSRF-TOKEN` ヘッダを自動送信するよう `springdoc.swagger-ui.csrf.enabled=true` を追加する。

## 技術上の制約（確定事項）

- セッションは STATELESS のため CSRF トークンは `CookieCsrfTokenRepository.withHttpOnlyFalse()`（`XSRF-TOKEN` クッキー）方式を用いる。
- `Secure` 属性は localhost の Swagger 確認でもブラウザが許容するため、そのまま付与する。
- application 層以下（AuthService / LoginResult / RefreshResult / domain / infrastructure）は変更禁止。生トークン文字列の受け渡しインターフェースは不変。
