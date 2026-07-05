# requirements

## 作業概要
Swagger UI から本 API を対話的に叩けるようにする。具体的には次を最小構成で実現する。

1. OpenAPI 設定 Bean（`config/OpenApiConfig`）を新規追加する（`@Bean OpenAPI` 方式）。
   - OpenAPI メタデータ（title / version / description）を定義する。
   - Bearer(JWT) 認証の SecurityScheme（type=http, scheme=bearer, bearerFormat=JWT, name=`bearerAuth`）を 1 件登録し、Swagger UI に「Authorize」ボタンを表示させる。
   - 現状の実装済みエンドポイント（`POST /login`・`POST /refresh`）はいずれも `permitAll` のため、これらには Bearer 要求（`@SecurityRequirement`）を **付けない**。認証必須エンドポイントが将来追加された際に `@SecurityRequirement("bearerAuth")` を付ける前提だけを整える（＝スキームの宣言のみ行い、グローバルな security requirement は設定しない）。
2. Swagger（`springdoc.api-docs` / `springdoc.swagger-ui`）を `dev` プロファイル限定で有効化し、デフォルト（本番想定）では無効化する。
3. ローカル HTTPS 前提での疎通手段を整える（`refreshToken` / `XSRF-TOKEN` が `Secure` 属性のため `http://localhost` では Cookie 往復が成立しない）。実装コードではなく設定（`server.ssl.*`）とドキュメントで解決する。
4. 各エンドポイントに `@Operation` / `@Schema` / リクエスト例を最小構成で付与する（`AuthController` と Request/Response DTO への、ロジック非変更のドキュメント用注釈）。

## 作業対象レイヤー
config（横断的関心事）。DDD の特定の業務レイヤーには属さない。

## 作業対象の種別
config（横断的関心事）。`docs/file-change-workflow.md`「### config（横断的関心事）の扱い」に基づく。主成果物は `config/OpenApiConfig`（`@Configuration`）。

> 単一 steering にまとめる根拠: `docs/file-change-workflow.md`「### config（横断的関心事）の扱い」の規定により、config は特定の業務レイヤーに属さないため、主成果物（新規 `@Configuration`）に加え、**ロジックを変更しない範囲で**複数レイヤーの既存ファイルへドキュメント用注釈・設定変更を横断してよい。本タスクの付随変更は (a) presentation 層 既存 `AuthController` / DTO への `@Operation`・`@Schema`（ロジック・シグネチャ非変更）、(b) `application*.properties` の有効化フラグ、(c) `server.ssl.*` による HTTPS 設定と docs のみで、いずれもロジック変更を伴わない。したがって単一 steering（種別 config）として扱う。

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 + AssertJ。config 種別のため単体テストは作らず、**結合テスト**（`@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles`）で間接検証する。
- 使用ライブラリ／アノテーション:
  - `org.springdoc:springdoc-openapi-starter-webmvc-ui`（pom に追加済み・2.8.17）。
  - `io.swagger.v3.oas.models.OpenAPI` / `Info` / `Components` / `SecurityScheme`（**`@Bean OpenAPI` 方式に統一**。`@OpenAPIDefinition` / `@SecurityScheme` アノテーション方式は採らない）。
  - presentation 側: `@Operation`・`@Schema`・`@Parameter`（`io.swagger.v3.oas.annotations.*`）。

## 隣接レイヤー
- 1 つ外側のレイヤー: なし（Swagger UI / OpenAPI クライアントが外部の呼び出し元。config Bean は Spring コンテナが解決する）。
- 1 つ内側のレイヤー: なし（`OpenApiConfig` はドメイン／サービスを呼ばない。メタデータのみを構築する）。

## 外側レイヤーとの契約
該当なし（config のため）。config は特定の業務レイヤーに属さず、外側の呼び出し元メソッドに合わせるべきシグネチャは存在しない。

## 作業対象メソッド／成果物のシグネチャ
作業概要を達成するための成果物。

### 新規: `config/OpenApiConfig`（`@Configuration`）— `@Bean OpenAPI` 方式
- メソッド名: `openAPI`
- 引数: なし
- 戻り値: `io.swagger.v3.oas.models.OpenAPI`
- 内容: `Info`（title / version / description）＋ `Components` に `SecurityScheme`（name=`bearerAuth`, type=HTTP, scheme=`bearer`, bearerFormat=`JWT`）を登録。グローバル `addSecurityItem` は **行わない**。

### 既存への付与（presentation 層・ロジック非変更のドキュメント用注釈）
実在シグネチャに合わせて付与する（変更しない）。
- `AuthController#login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse)` → 戻り値 `ResponseEntity<LoginResponse>`。`@Operation`（summary/description）を付与。
- `AuthController#refresh(String refreshToken)`（`@CookieValue("refreshToken")`）→ 戻り値 `ResponseEntity<RefreshResponse>`。`@Operation` ＋ Cookie パラメータ説明を付与。
- `LoginRequest`（フィールド: `tenantCode` / `email` / `password`、いずれも `@NotBlank`）に `@Schema`（説明・example）を付与。
- `LoginResponse` / `RefreshResponse`（各 `accessToken` ＋ `"Bearer"` トークン種別）に `@Schema` を付与。
- `AuthController` の login/refresh には `@SecurityRequirement` を付けない（両者 permitAll のため）。

### 設定・プロファイル制御（`dev` プロファイルのみ）
- ベース `application.properties`: `springdoc.api-docs.enabled=false` / `springdoc.swagger-ui.enabled=false`（デフォルト無効＝本番想定）。既存の `springdoc.swagger-ui.csrf.enabled=true` は維持。
- 新規 `application-dev.properties`: `springdoc.api-docs.enabled=true` / `springdoc.swagger-ui.enabled=true`。加えて HTTPS 設定（下記）を置く。
- HTTPS（`dev` プロファイル）: `server.ssl.enabled=true` / `server.ssl.key-store` / `server.ssl.key-store-password` / `server.ssl.key-store-type=PKCS12` / `server.ssl.key-alias` を設定する。
  - 自己署名 PKCS12 keystore と keystore-password は **リポジトリにコミットしない**。各自 `keytool` でローカル生成し、`key-store` のパス・`key-store-password` は env プレースホルダ（例: `${SSL_KEYSTORE_PATH}` / `${SSL_KEYSTORE_PASSWORD}`）で注入する。
  - `keytool` による自己署名証明書生成手順と、HTTPS（`https://localhost:8443` 等）での Swagger UI アクセス手順を docs もしくは README に記載する。
- HTTPS 前提を維持し、`dev` で Cookie の `Secure` を外すコード分岐は **採らない**（`AuthController#buildRefreshTokenCookie` のロジックは変更しない）。

## 内側レイヤーへの契約
該当なし（config のため）。`docs/file-change-workflow.md`「### config（横断的関心事）の扱い」および判定早見表の config 行に従い、内側スタブ・Command の作成、TODO.md 登録はいずれも発生しない。

## ドメインモデルの利用
該当なし（config のため）。DomainObject / ValueObject の作成・利用は発生しない。`OpenApiConfig` はメタデータのみを構築し、ドメイン層に依存しない。

## テスト方針（設定系の検証をどう担保するか）
config 種別は単体テスト対象外のため、結合テストで間接検証する。test プロファイル（`application-test.properties`）は汚さず、springdoc の有効化フラグは `@TestPropertySource`（または `@DynamicPropertySource`）でテストクラス単位に上書きする。
- 追加する結合テスト（`@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`）:
  - springdoc 有効化フラグを `@TestPropertySource` で `true` に上書きしたケースで、`GET /v3/api-docs` が 200 を返し、レスポンス JSON に
    - `info.title` / `info.version`、
    - `components.securitySchemes.bearerAuth`（type=http, scheme=bearer, bearerFormat=JWT）
    が含まれること（正常系）。
  - 同ケースで `/login`・`/refresh` の path に security requirement が **付いていない**ことを確認する（正常系・Bearer 非要求方針の担保）。
  - springdoc 有効化フラグを上書きしない（ベース `application.properties` 由来で無効）ケースで `GET /v3/api-docs` が 404 になること（異常系・無効化の担保）。
- 純設定（properties / HTTPS）は既存の `LoginIntegrationTest` / `MultiTenantAuthApiApplicationTests`（`@SpringBootTest`）のコンテキスト起動により、`OpenApiConfig` 追加でコンテキストが壊れないことを回帰で担保する。

## 未決定事項
なし（6 点すべて確定）。
