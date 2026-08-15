# tasklist

- [x] `StubAccessTokenProvider.java` を削除する
- [x] `JwtProperties` record を新規作成する（`@ConfigurationProperties(prefix = "jwt")`、フィールド: `secret:String`、`expiration:Duration`）
- [x] `JwtConfig.java` を新規作成する（`@Configuration` + `@EnableConfigurationProperties(JwtProperties.class)`）
- [x] `JwtAccessTokenProvider` を新規作成する（`implements AccessTokenProvider`、`@Component`、`JwtProperties` をコンストラクタインジェクション、`issue(User user)` を jjwt HS256 で実装）
- [x] `JwtProperties`・`JwtConfig`・`JwtAccessTokenProvider` に Javadoc を記載する
- [x] 正常系のテストコードを記載する（非 null / 非空の返却値、JWT クレームの検証: `sub`・`tenantId`・`role`・`exp` の期待値一致）
- [x] 異常系のテストコードを記載する（`user = null` → `NullPointerException`）
- [x] 内側レイヤーに作成したスタブはないため、TODO.md への追記は不要（スキップ）
- [x] TODO.md の `infrastructure.security` セクション `[ ] StubAccessTokenProvider を jjwt 実装へ置換` のチェックボックスを埋める（完了にする）
- [x] TODO.md の `Port (domain)` セクション `[ ] AccessTokenProvider#issue` のチェックボックスを埋める（完了にする）
