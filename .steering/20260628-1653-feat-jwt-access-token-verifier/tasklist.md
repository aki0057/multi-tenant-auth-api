# tasklist

- [x] `StubAccessTokenVerifier.java` を削除する
- [x] `AccessTokenVerifier.java` 末尾の `// TODO: 増分2（infrastructure.security の JwtAccessTokenVerifier）で本実装する` コメントを削除する
- [x] `JwtAccessTokenVerifier` を新規作成する（`implements AccessTokenVerifier`、`@Component`、`JwtProperties` をコンストラクタインジェクション、`Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8))` で `SecretKey` 構築、`verify(String token)` を jjwt `parseSignedClaims` で実装し例外はそのまま伝播）
- [x] `JwtAccessTokenVerifier` に Javadoc を記載する
- [x] 正常系のテストコードを記載する（有効トークン往復検証: `JwtAccessTokenProvider#issue` で発行したトークンを `verify` し `userId` / `tenantId` / `role` が一致すること）
- [x] 異常系のテストコードを記載する（署名不正（別鍵） → `SignatureException` 伝播）
- [x] 異常系のテストコードを記載する（期限切れトークン → `ExpiredJwtException` 伝播）
- [x] 異常系のテストコードを記載する（不正フォーマット文字列 → `MalformedJwtException` 等伝播）
- [x] 異常系のテストコードを記載する（alg=none 未署名トークン → `UnsupportedJwtException` 伝播）
- [x] 内側レイヤーに作成したスタブはないため、TODO.md への追記は不要（スキップ）
- [x] TODO.md の `infrastructure.security` セクション `[ ] StubAccessTokenVerifier を JwtAccessTokenVerifier へ置換` のチェックボックスを埋める（完了にする）
- [x] TODO.md の `Port (domain)` セクション `[ ] AccessTokenVerifier#verify` のチェックボックスを埋める（完了にする）
