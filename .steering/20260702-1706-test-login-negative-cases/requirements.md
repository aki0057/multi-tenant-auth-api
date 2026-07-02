# requirements

## 作業概要
既存の結合テスト `src/test/java/io/github/aki0057/multitenant/auth/LoginIntegrationTest.java`（`/login` エンドポイントの結合テスト）に、次の5つの異常系テストケースを追加する。

1. パスワード不一致 → 401 Unauthorized
2. ユーザー不在 → 401 Unauthorized
3. アカウント無効（`users.is_active = false`）→ 401 Unauthorized
4. テナント無効（`tenants.is_active = false`）→ 401 Unauthorized
5. 必須項目欠落（`LoginRequest` の `@NotBlank` 違反）→ 400 Bad Request

**注記（本タスクの特記事項）**: `AuthController` / `AuthService` / `User#authenticate` / `LoginRequest` の `@NotBlank` バリデーション等、対象の本体実装は既に完成・コミット済みである（`git log` 参照: `95fc8b5`, `eeda23c`, `203b3c2` 等で該当の異常系ハンドリングは実装済み）。本タスクで追加するのは **テストコードのみ**であり、`src/main/` 配下の本体実装・VO・インターフェースへの変更は一切行わない。

## 作業対象レイヤー
presentation(API) ※結合テスト（横断）— `testing-guidelines.md` の「結合テスト（横断）」区分に該当する既存テストクラスへの追加

## 作業対象の種別
API

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 (Jupiter)。既存クラスと同様、AssertJ は使用せず `MockMvc` の `andExpect(status()....)` でアサーションする。
- Spring MVC アノテーション: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` + `@Transactional`（既存クラス宣言をそのまま利用。クラスへの追加変更は不要）。

## 隣接レイヤー
- 1 つ外側のレイヤー: なし（HTTP クライアント相当。`MockMvc` 経由で `POST /login` を呼び出す、既存クラスと同じ方式）
- 1 つ内側のレイヤー: application(`AuthService#login`) 以下、既存のフルコンテキスト（DB は H2、`@ActiveProfiles("test")`）。今回のテストは内側レイヤーを一切変更せず、既存実装をそのまま経由させて検証する。

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド（テスト対象は既存の API エンドポイント。変更しない）。
- メソッド名: `AuthController#login`
- 引数: `@Valid @RequestBody LoginRequest request`（`LoginRequest` は `tenantCode` / `email` / `password` の3フィールド、いずれも `@NotBlank`）
- 戻り値: `ResponseEntity<LoginResponse>`（マッピング: `@PostMapping("/login")`）

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
内側（`application.AuthService#login(LoginCommand command): String`、および `domain.model.User#authenticate`、`domain.repository.UserRepository#findByTenantCodeAndEmail`）はすべて既存実装であり、新規スタブの作成は不要。テストは既存の `AuthService` Bean をフルコンテキストで経由させ、DB（H2, `@ActiveProfiles("test")`）に投入したテストデータの組み合わせによって各異常系分岐（パスワード不一致・ユーザー不在・アカウント無効・テナント無効・バリデーション違反）を発火させる。

## テストケースごとの実現方法（テストデータ・期待結果）
既存の `@BeforeEach setUp()` は `tenants`（code=`testTenant`, is_active=true）と `users`（email=`test@example.com`, password=`password` の BCrypt ハッシュ, is_active=true）を1件ずつ投入する。追加する5テストは、必要に応じて各テストメソッド内で `jdbcTemplate` により追加データを投入する（既存クラス冒頭の `@Transactional` によりテスト後は自動ロールバックされるため、後始末の DELETE は不要）。

1. **パスワード不一致 → 401**: 既存の `setUp()` データ（`testTenant` / `test@example.com`）に対し、`password` に誤った値（例: `"wrongPassword"`）を送信する。追加データ投入は不要。
2. **ユーザー不在 → 401**: 既存の `setUp()` データのテナント（`testTenant`）に対し、未登録の `email`（例: `"notfound@example.com"`）を送信する。追加データ投入は不要。
3. **アカウント無効 → 401**: テストメソッド内で `jdbcTemplate` により `users.is_active = false` の追加ユーザー（既存とは別の email、例: `"inactive-user@example.com"`、同一テナント `testTenant`）を投入し、そのユーザーの email + 正しいパスワードで送信する。
4. **テナント無効 → 401**: テストメソッド内で `jdbcTemplate` により `tenants.is_active = false` の追加テナント（既存とは別の code、例: `"inactiveTenant"`）と、そのテナント配下に `is_active = true` の追加ユーザー（既存とは別の email）を投入し、そのテナントコード + email + 正しいパスワードで送信する。
5. **必須項目欠落 → 400**: `LoginRequest` の必須フィールド（`password` を欠落させる想定。JSON ボディから `password` キー自体を省略する）以外は正しい値を送信する。追加データ投入は不要。

いずれも `status().isUnauthorized()`（401）または `status().isBadRequest()`（400）のみをアサーションし、レスポンスボディの詳細検証は既存クラスの方針（正常系・既存異常系テスト）に合わせて行わない。
