# requirements

## 作業概要
ログアウトエンドポイント `POST /auth/logout` を presentation 層に新規作成する。`refreshToken` クッキーで提示されたリフレッシュトークンを application 層へ渡して失効（削除）させ、クライアント側のクッキーを削除するために `Set-Cookie`（`refreshToken` と `XSRF-TOKEN` の両方を Max-Age=0 で失効）を返す。成功時は常に 204 No Content を返す。クッキー未提示・トークン不明・失効済み・期限切れのいずれのケースでも 204 を返す（冪等）。

あわせて、リフレッシュトークン Cookie の Path を発行・削除で統一するため、同一クラス `AuthController` 内の既存メソッド `buildRefreshTokenCookie`（発行側、現在 `Path=/auth/refresh`）を `Path=/auth` に変更する。これにより login / refresh で発行される Cookie と logout で削除する Cookie の Path が一致し、ブラウザ上でのクッキー削除が確実に行える。この変更は同じ presentation 層・同一クラス内の修正のため本 steering に含める。

なお、`/auth/logout` を CSRF 保護有効チェーンへ追加し permitAll とする `config/SecurityConfig` の修正は、種別 `config` の別 steering（`.steering/20260719-1819-config-security-logout-chain/`）で扱う。

## 作業対象レイヤー
presentation (API / Controller)

## 作業対象の種別
API

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 + Mockito（Controller テストは `@WebMvcTest(AuthController.class)` + `@Import({SecurityConfig.class, PasswordEncoderConfig.class, GlobalExceptionHandler.class})`、Service は `@MockitoBean`）。アサーションは AssertJ。
- Spring MVC アノテーション: `@PostMapping("/auth/logout")`、`@CookieValue(value = "refreshToken", required = false)`、`@Operation` / `@Parameter`（Swagger）

## 隣接レイヤー
- 1 つ外側のレイヤー: なし（API が最も外側で、呼び出し元は外部クライアント）
- 1 つ内側のレイヤー: application（`AuthService`）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
該当なし（作業対象が API のため）。

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド。
- メソッド名: `logout`
- 引数: `@CookieValue(value = "refreshToken", required = false) String refreshToken`
- 戻り値: `ResponseEntity<Void>`（204 No Content）

補足（レスポンスの Set-Cookie 仕様、ユーザー決定事項）:
- `refreshToken` を削除する `Set-Cookie`: `Max-Age=0; Path=/auth`。その他の属性（`HttpOnly; Secure; SameSite=Strict`）は発行時と同一とする。
- 発行側の Path 統一: 同一クラス `AuthController` の既存メソッド `buildRefreshTokenCookie` の `Path` を `/auth/refresh` から `/auth` へ変更する（login / refresh の発行 Cookie と logout の削除 Cookie の Path を一致させるため）。login / refresh のレスポンス内容はこの Path 変更以外は変えない。
- `XSRF-TOKEN` を削除する `Set-Cookie`: `Max-Age=0`。XSRF-TOKEN は JS から参照可能なため `HttpOnly=false`。
- クッキー未提示（`refreshToken` が null）・トークン不明・失効済み・期限切れのいずれでも 204 を返す。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
内側はまだ存在しないため、スタブとして新規作成する（実装しない・TODO のみ）。
- メソッド名: `AuthService#logout`
- 引数: `LogoutCommand`（入力 DTO の Command。プリミティブ `String refreshToken` を保持する record）
- 戻り値: `void`
- 補足: 提示された生リフレッシュトークンを application 層で受け取り、ハッシュ化して該当レコードを失効/削除する（既存の `refresh` と同様、プリミティブ → ValueObject / TokenHash 変換は Service 入口で行う）。トークン不明・失効済み・期限切れ・null のいずれでも例外を投げず冪等に完了する。
- Command の扱い: `LogoutCommand` はこのステップで作成するが、スタブ（実装しない・TODO のみ）ではなく作成と同時に完成扱いとし、`TODO.md` には登録しない。詳細は `docs/file-change-workflow.md` の「Command（Service の入力 DTO）の扱い」を参照。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
該当なし（作業対象が API のため）。プリミティブ → ValueObject 変換は内側の `AuthService#logout` の入口で行う。
