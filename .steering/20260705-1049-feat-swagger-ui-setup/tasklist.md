# tasklist

> 種別: config（横断的関心事）。`docs/file-change-workflow.md`「### config（横断的関心事）の扱い」および判定早見表の config 行（すべて ─）に基づく。
> DDD 契約項目は非該当: スタブ / Command / DomainObject / ValueObject の作成、TODO.md 登録、外側・内側レイヤーとの契約はいずれも発生しない（該当タスクは設けない）。
> テストは単体を作らず結合テストで間接検証する。既存の presentation 層への注釈付与は「ロジック非変更のドキュメント用注釈」であり、新規実装向けの Javadoc/正常系/異常系テストは課さない。

## 1. OpenAPI 設定（主成果物、`@Bean OpenAPI` 方式）
- [x] `config/OpenApiConfig`（`@Configuration`）を新規作成し、`@Bean` メソッド `public OpenAPI openAPI()` で OpenAPI メタデータ（title / version / description）を定義する
- [x] `openAPI()` の `Components` に Bearer(JWT) の SecurityScheme（name=`bearerAuth`, type=HTTP, scheme=`bearer`, bearerFormat=`JWT`）を登録する（グローバル `addSecurityItem` は付与しない。`@OpenAPIDefinition`/`@SecurityScheme` アノテーション方式は使わない）
- [x] `OpenApiConfig` に Javadoc を記載する

## 2. プロファイル制御（設定、`dev` プロファイルのみ）
- [x] ベース `application.properties` に `springdoc.api-docs.enabled=false` / `springdoc.swagger-ui.enabled=false` を追加する（デフォルト無効＝本番想定。既存の `springdoc.swagger-ui.csrf.enabled=true` は維持）
- [x] 新規 `src/main/resources/application-dev.properties` を作成し、`springdoc.api-docs.enabled=true` / `springdoc.swagger-ui.enabled=true` を設定する

## 3. ローカル HTTPS（`dev` プロファイル設定＋ドキュメント）
- [x] `application-dev.properties` に `server.ssl.enabled=true` / `server.ssl.key-store=${SSL_KEYSTORE_PATH}` / `server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}` / `server.ssl.key-store-type=PKCS12` / `server.ssl.key-alias=...` を設定する（keystore とパスワードは env プレースホルダ注入。リポジトリにコミットしない）
- [x] `keytool` による自己署名 PKCS12 証明書の生成手順と、HTTPS での Swagger UI アクセス手順（例: `https://localhost:8443/swagger-ui.html`）を docs もしくは README に記載する
- [x] keystore ファイルが誤ってコミットされないよう `.gitignore` を確認・必要なら追記する

## 4. エンドポイントのドキュメント化（presentation 層・ロジック非変更のドキュメント用注釈）
- [x] `AuthController#login` / `#refresh` に `@Operation`（および `/refresh` の Cookie パラメータ説明）を付与する
- [x] `LoginRequest` / `LoginResponse` / `RefreshResponse` に `@Schema`（説明・example）を付与する
- [x] 付与した注釈が既存の実在シグネチャ・ロジックを変更していないことを確認する（login/refresh には `@SecurityRequirement` を付けない。`buildRefreshTokenCookie` の `Secure` は変更しない）

## 5. テスト（OpenApiConfig／設定に対する結合テストで間接検証、test プロファイルは汚さない）
- [x] `@TestPropertySource`（または `@DynamicPropertySource`）で springdoc フラグを `true` に上書きしたケースで、`GET /v3/api-docs` が 200 を返し `info.title`/`info.version` と `components.securitySchemes.bearerAuth`（http/bearer/JWT）を含むことを検証する結合テストを追加する
- [x] 同ケースで `/login`・`/refresh` の path に security requirement が付いていないことを検証する（Bearer 非要求方針の担保）
- [x] springdoc フラグを上書きしない（ベース由来で無効な）ケースで `GET /v3/api-docs` が 404 になることを検証する（無効化の担保）

## 6. 回帰
- [ ] `./mvnw clean test` で既存の結合テスト（`LoginIntegrationTest` / `MultiTenantAuthApiApplicationTests`）を含め全テストが green であることを確認する
