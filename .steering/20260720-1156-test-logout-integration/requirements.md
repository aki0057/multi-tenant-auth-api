# requirements

## 作業概要
既に実装済みの logout エンドポイント（`POST /auth/logout`）に対する結合テスト（横断）を 1 クラス作成する。presentation → application → domain → infrastructure（H2）までフルコンテキストで起動し、HTTP 入口から DB のリフレッシュトークン失効までを一気通貫で検証する。成果物はテストクラスのみで、`src/main/` の業務コードは一切変更しない。

- テストクラス: `src/test/java/io/github/aki0057/multitenant/auth/LogoutIntegrationTest.java`
- 既存の `LoginIntegrationTest`（同パッケージ）の構成・命名・テストデータ用意方法に合わせる。

対象エンドポイントの確定仕様（`AuthController#logout` / `AuthService#logout` / `SecurityConfig` から実測）:
- `POST /auth/logout` は `refreshToken` クッキー（`required = false`、未提示時は `null`）を受け取り、`AuthService#logout` へ委譲する。
- 成功時は本文なしの **204 No Content**。`refreshToken` を `Max-Age=0` で失効させる `Set-Cookie` と、`XSRF-TOKEN` の削除クッキーを付与する。
- `AuthService#logout` は**冪等**（例外を投げない）。`refreshToken` が `null` / 形式不正 / DB 未一致でも 204 を返す。該当レコードがある場合のみ `is_revoked = true` へ更新する（物理削除しない、対象は `token_hash` 一致の 1 件のみ）。
- `/auth/logout` は CSRF 保護有効チェーン（`SecurityConfig` の `@Order(1)` `refreshSecurityFilterChain`、`CookieCsrfTokenRepository.withHttpOnlyFalse()` + `CsrfTokenRequestAttributeHandler`）に属する。CSRF トークン（`XSRF-TOKEN` クッキー値を `X-XSRF-TOKEN` ヘッダで送信）が無い / 不一致だと **403 Forbidden**。
- `token_hash` は生トークンの SHA-256（`Sha256RefreshTokenHasher`）で決まるため、有効なリフレッシュトークンの用意は「先に `POST /auth/login` を実行して実クッキー（`refreshToken` / `XSRF-TOKEN`）を取得する」方式が最も確実（`JdbcTemplate` で `refresh_tokens` を直接 INSERT する場合は生トークンと `token_hash` の整合が取れない点に留意）。

参考テーブル（`refresh_tokens`）: `id, tenant_id, user_id, token_hash(UNIQUE), expires_at, is_revoked(default false), created_at, updated_at, created_by, updated_by`。

## 作業対象レイヤー
結合テスト（横断）。特定の業務レイヤーに属さず、presentation → application → domain → infrastructure を跨いで検証する。

## 作業対象の種別
結合テスト（横断）（`docs/testing-guidelines.md`「レイヤー別テスト方針」の「結合テスト（横断）」行に対応。DDD の業務レイヤーには属さない横断的なテスト作成であり、判定早見表の config 行と同様に DDD 契約項目はいずれも非該当）。

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 (Jupiter) + AssertJ（`assertThat`）。モック（Mockito）は使用しない（フルコンテキスト）。
- Spring アノテーション: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` + `@Transactional`。
- HTTP: `MockMvc`。テストデータ: `@BeforeEach` / `@AfterEach` で `JdbcTemplate` により直接 INSERT / DELETE（`@Transactional` によるロールバックと併用は `LoginIntegrationTest` に準拠）。パスワードハッシュは `PasswordEncoder#encode` を実行時生成。

## 隣接レイヤー
- 1 つ外側のレイヤー: 該当なし（結合テストのため。HTTP クライアント（MockMvc）が外側に相当するが業務レイヤーではない）
- 1 つ内側のレイヤー: 該当なし（結合テストのため。フルコンテキストで presentation 以下すべてが実体として起動する）

## 外側レイヤーとの契約
該当なし（結合テストのため、`src/main/` の業務コードは変更しない）。テスト対象は HTTP エンドポイント `POST /auth/logout` の観測可能な振る舞い（ステータスコード・Set-Cookie・DB 状態）であり、既存実装のシグネチャに合わせて検証する（変更しない）。

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一成果物（テストクラス）。
- クラス名: `LogoutIntegrationTest`（`io.github.aki0057.multitenant.auth`）
- 検証対象エンドポイント: `POST /auth/logout`（`refreshToken` クッキー任意 + CSRF: `X-XSRF-TOKEN` ヘッダ）
- 各テストメソッド: 引数なし・戻り値 `void`（`throws Exception`）。命名は `logout_<condition>()`、`@DisplayName` は日本語で `正常系:` / `異常系:` 始まり。

## 内側レイヤーへの契約
該当なし（結合テストのため）。スタブ / Command の新規作成は発生しない（`LogoutCommand` を含む対象コードは実装済み）。

## ドメインモデルの利用
該当なし（結合テストのため）。DomainObject / ValueObject の新規作成・利用は発生しない。
