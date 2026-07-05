# tasklist

- [x] (条件: 作業対象が TODO.md に存在しない場合) 作業対象メソッド名を、プロジェクトルートの TODO.md の 作業対象レイヤーの章に、空のチェックボックスで追記する
      ※ `AuthController#login` / `AuthController#refresh` は TODO.md の「API (presentation)」章に既に存在するため、通常はスキップ。→ 既存のためスキップ。
- [x] 作業対象を実装する
      - [x] AuthController#login: リフレッシュトークンを `Set-Cookie`（`refreshToken; HttpOnly; Secure; SameSite=Strict; Path=/refresh`）で返し、JSON ボディから除去する。login レスポンス時点で `XSRF-TOKEN` を発行する。
      - [x] AuthController#refresh: 入力を `@CookieValue("refreshToken")` に変更し、ローテーション後トークンを `Set-Cookie` で返し、JSON ボディから除去する。
      - [x] LoginResponse / RefreshResponse から `refreshToken` を削除する。
      - [x] 未使用となった RefreshRequest を削除する。
      - [x] SecurityConfig を `/refresh` 用チェーン（CSRF 有効・CookieCsrfTokenRepository.withHttpOnlyFalse）とデフォルトチェーン（CSRF 無効）に分割する。
      - [x] GlobalExceptionHandler に `MissingRequestCookieException` → 401 のハンドラを追加する。
      - [x] application.properties に `springdoc.swagger-ui.csrf.enabled=true` を追加する。
- [x] Javadoc を記載する（変更した各メソッド / DTO / 設定クラスのドキュメントを更新する）
- [x] 正常系のテストコードを記載する
      - [x] login: 200 OK、ボディに accessToken / token="Bearer"、`refreshToken` がボディに含まれない、`Set-Cookie` に `refreshToken`（HttpOnly / Secure / SameSite=Strict / Path=/refresh）が付与される。
      - [x] login: レスポンスの `Set-Cookie` に `XSRF-TOKEN` クッキーが含まれる（鶏卵問題回避のため login レスポンス時点で XSRF-TOKEN を発行する、という設計要点の担保）。
      - [x] refresh: Cookie からリフレッシュトークンを受け取り 200 OK、ボディに accessToken / tokenType、ローテーション後トークンが `Set-Cookie` で返る。
- [x] 異常系のテストコードを記載する
      - [x] refresh: `refreshToken` Cookie 未送付時に 401 が返る（500 にならない）。
      - [x] refresh: CSRF トークン（`X-XSRF-TOKEN` ヘッダ）なしで `/refresh` へ POST した場合に 403 が返る（「/refresh のみ CSRF 保護を有効化」という中核要件の担保。CSRF が誤って無効のままでも正常系は通ってしまうため失敗系で保証する）。
      - [x] login: 認証失敗（BadCredentialsException）で 401、バリデーション失敗で 400、予期しない例外で 500（既存挙動の回帰）。
- [x] (条件: 作業対象が ValueObject でも Repository でもない場合) 内側レイヤーに作成したスタブのメソッド名を、TODO.md の該当する層の章に、空のチェックボックスで追記する
      ※ 内側の AuthService は既存・不変で新規スタブを作成しないため、追記対象なし（スキップ）。
- [x] (条件: DomainObject / ValueObject を作成した場合) 作成した DomainObject 名を、TODO.md の該当する層の章に、空のチェックボックスで追記する
      ※ 本作業では DomainObject / ValueObject を作成しないため、スキップ。
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックスを埋める（完了にする）
      ※ `AuthController#login` / `AuthController#refresh` は既に `[x]`。状態が変わらなければ変更不要。→ 既に `[x]` のため変更なし。
