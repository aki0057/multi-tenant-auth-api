# tasklist

config 種別のため、DDD 契約項目（スタブ作成 / Command / DomainObject / ValueObject の作成、TODO.md 登録、内側/外側レイヤーとの契約）はいずれも発生しない。
テンプレートの条件付き項目（TODO.md 追記、内側スタブ登録、DomainObject 登録、TODO.md チェック埋め）はすべて非該当のため含めない。
単体テストは作らないため「正常系/異常系のテストコードを記載する」も含めない。回帰は既存結合テストのコンテキスト起動で担保する（段階4 tester が確認）。
無条件テンプレ項目である「Javadoc を記載する」も含めない。本タスクは新規クラス/メソッドを作成せず、既存 properties/テストファイルの編集・削除のみのため、Javadoc の記載対象が存在しない（無条件テンプレ項目だが非該当）。

- [x] `src/main/resources/application.properties` から `springdoc.api-docs.enabled=false` と `springdoc.swagger-ui.enabled=false` の 2 行を削除する（Swagger UI をデフォルト有効化）
- [x] `src/main/resources/application.properties` の `springdoc.swagger-ui.csrf.enabled=true` を維持する（削除しない）
- [x] `src/main/resources/application.properties` に HTTPS ブロックを追加する（`server.ssl.enabled=true` / `server.ssl.key-store=${SSL_KEYSTORE_PATH:}` / `server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD:}` / `server.ssl.key-store-type=PKCS12` / `server.ssl.key-alias=localhost` / `server.port=8443`。プレースホルダは空デフォルト付き）
- [x] `src/test/resources/application-test.properties` に `server.ssl.enabled=false` を追記する（全 @SpringBootTest のコンテキスト起動を保証する回帰対策）
- [x] `src/main/resources/application-dev.properties` を削除する（dev ゲーティング撤去）
- [x] `src/test/java/io/github/aki0057/multitenant/auth/OpenApiDocsIntegrationTest.java` を削除する（デフォルト有効化により冗長）
- [x] `src/test/java/io/github/aki0057/multitenant/auth/OpenApiDocsDisabledIntegrationTest.java` を削除する（GET /v3/api-docs 404 期待がデフォルト有効化と矛盾）
