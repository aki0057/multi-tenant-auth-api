# requirements

## 作業概要
Swagger UI を「dev プロファイルによるゲーティング」構成から「本番（デフォルト）プロファイルで有効」構成へ最小化するリファクタリングを行う。
既存 steering（`.steering/20260705-1049-feat-swagger-ui-setup/`）で導入された over-engineered な dev ゲーティング（`application-dev.properties` による Swagger 有効化、および OpenAPI 有効/無効を検証する専用結合テスト）を撤去し、springdoc starter のデフォルト挙動（Swagger UI 有効）に戻す。
ゴールは、Spring を本番（デフォルト）モードで起動したまま、自己署名 HTTPS 経由で Swagger UI から `/login`・`/refresh` を E2E 疎通できる最小構成に戻すこと。
keystore ファイルとパスワードはリポジトリにコミットせず、env プレースホルダで注入する。

具体的な src/ 変更スコープ:
1. `src/main/resources/application.properties`
   - `springdoc.api-docs.enabled=false` と `springdoc.swagger-ui.enabled=false` の 2 行を削除する（springdoc starter によりデフォルトで Swagger UI が有効になる）。
   - `springdoc.swagger-ui.csrf.enabled=true` は維持する（/refresh の CSRF 対応）。
   - HTTPS ブロックを追加する:
     - `server.ssl.enabled=true`
     - `server.ssl.key-store=${SSL_KEYSTORE_PATH:}`
     - `server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD:}`
     - `server.ssl.key-store-type=PKCS12`
     - `server.ssl.key-alias=localhost`
     - `server.port=8443`
   - プレースホルダに空デフォルト（`${...:}`）を付ける理由: テストプロファイル（`application-test.properties`）はベース `application.properties` を継承する。既存の DB プレースホルダは test で具体値に上書きされるため失敗しないが、SSL プレースホルダは test に上書き対象がないため、空デフォルトを付けないと test コンテキスト起動時にプレースホルダ解決失敗で全 `@SpringBootTest` が壊れる。これを回避する。
2. `src/test/resources/application-test.properties`
   - `server.ssl.enabled=false` を追記する（全テストは `@SpringBootTest` の MOCK 環境。SSL を明示的に無効化してコンテキスト起動を保証する回帰対策）。
3. `src/main/resources/application-dev.properties` を削除する（dev プロファイルによるゲーティングを撤去）。
4. `src/test/java/io/github/aki0057/multitenant/auth/OpenApiDocsIntegrationTest.java` と `OpenApiDocsDisabledIntegrationTest.java` を削除する。
   - 前者は springdoc をデフォルト有効化したため冗長。
   - 後者は `GET /v3/api-docs` が 404 になることを期待するが、デフォルト有効化と矛盾するため削除する。
   - 新規の代替テストは追加しない（config 種別のため単体テストは作らず、既存の `LoginIntegrationTest` / `MultiTenantAuthApiApplicationTests` のコンテキスト起動で `OpenApiConfig` の回帰を担保する）。

### 変更しないもの（現状維持・タスク化しない）
- `src/main/java/io/github/aki0057/multitenant/auth/config/OpenApiConfig.java`（そのまま残す）
- `AuthController` / `LoginRequest` / `LoginResponse` / `RefreshResponse` の `@Operation` / `@Schema` / `@Parameter` 注釈（そのまま残す）

### この steering のスコープ外（記載のみ・タスク化しない）
- `README.md` の本番向け修正、`.gitignore` の keystore 無視エントリはメインセッションが src 外で別途担当する。implementer のタスクには含めない。
- `docs/file-change-workflow.md` はユーザーが手動修正するため触らない。

## 作業対象レイヤー
config（横断的関心事。特定の業務レイヤーに属さない）

## 作業対象の種別
config

## 使用するテスト・フレームワーク等
- テストフレームワーク: なし（config 種別のため単体テストは作らない）。既存結合テスト（`@SpringBootTest`）のコンテキスト起動で間接的に回帰担保する。
- Spring MVC アノテーション: なし（properties ファイルの編集・削除、およびテストクラスの削除のみ。Java 設定クラスの新規作成なし）

## 隣接レイヤー
- 1 つ外側のレイヤー: 該当なし（config のため。横断的関心事であり呼び出し元となる業務レイヤーを持たない）
- 1 つ内側のレイヤー: 該当なし（config のため。呼び出し先となる業務レイヤーを持たない）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
該当なし（config のため）。properties の編集・削除およびテストクラスの削除であり、外側レイヤーのメソッドシグネチャに合わせる契約は発生しない。

## 作業対象メソッドのシグネチャ
該当なし（config のため）。メソッドの実装は行わない。成果物は以下の設定ファイル/テストファイルの編集・削除のみ。
- 編集: `src/main/resources/application.properties`（springdoc の 2 行削除、HTTPS ブロック追加、csrf 行維持）
- 追記: `src/test/resources/application-test.properties`（`server.ssl.enabled=false`）
- 削除: `src/main/resources/application-dev.properties`
- 削除: `src/test/java/io/github/aki0057/multitenant/auth/OpenApiDocsIntegrationTest.java`
- 削除: `src/test/java/io/github/aki0057/multitenant/auth/OpenApiDocsDisabledIntegrationTest.java`

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
該当なし（config のため）。スタブの新規作成・TODO.md 登録は発生しない。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
該当なし（config のため）。DomainObject / ValueObject の作成・利用は発生しない。

## 回帰
- `./mvnw clean test` で既存の結合テストを含め全テストが green であることを段階4（tester）で確認する。
- 特に、SSL プレースホルダの空デフォルトと `application-test.properties` の `server.ssl.enabled=false` により、全 `@SpringBootTest`（MOCK 環境）のコンテキストが正常起動することを担保する。
