# tasklist

## TODO.md への事前追記

- [x] `RefreshTokenExpirationPolicy#expiration` を、TODO.md の `Port (domain)` 章に、空のチェックボックスで追記する
- [x] `StubRefreshTokenExpirationPolicy を実装へ置換` を、TODO.md の `infrastructure.security` 章に、空のチェックボックスで追記する

## RefreshToken への tenantId 追加

- [x] `domain/model/RefreshToken.java` に `TenantId tenantId` フィールドを `id` の次に追加する（フィールド順: `id, tenantId, userId, tokenHash, expiresAt, revoked`）
- [x] `revoke()` を `tenantId` を含む全フィールドを引き継ぐよう修正する
- [x] `RefreshToken.java` の Javadoc（クラス Javadoc・`@param`）を更新する
- [x] `RefreshTokenTest.java` の `newToken(...)` ヘルパーおよび各テストの `new RefreshToken(...)` 呼び出しに `TenantId` を追加する
- [x] `revoke_returnsRevokedInstance` に `tenantId` の引き継ぎを検証するアサーションを追加する

## 新設ポート RefreshTokenExpirationPolicy

- [x] `domain/service/RefreshTokenExpirationPolicy.java` を新規作成する（`Duration expiration()` を持つインターフェース）。本増分では未実装のため、既存の未実装ポート（`RefreshTokenGenerator.java` / `RefreshTokenHasher.java`）と同じ規約でインターフェース宣言直上に `// TODO: 実装（後続 infrastructure 増分）` を付与する
- [x] Javadoc を記載する

## infrastructure スタブ

- [x] `infrastructure/security/StubRefreshTokenExpirationPolicy.java` を新規作成する（`@Component`、`implements RefreshTokenExpirationPolicy`、`expiration()` は常に `UnsupportedOperationException` をスロー、クラス直上に `// TODO: JwtProperties 参照実装へ置換（後続 infrastructure 増分）` を記載）
- [x] Javadoc を記載する

## AuthService の修正

- [x] `AuthService.java` の `REFRESH_TOKEN_EXPIRATION = Duration.ofDays(14)` 定数を削除する
- [x] `AuthService.java` に `RefreshTokenExpirationPolicy` フィールドを追加する（`@RequiredArgsConstructor` によるコンストラクタ注入）
- [x] `now.plus(REFRESH_TOKEN_EXPIRATION)` を `now.plus(refreshTokenExpirationPolicy.expiration())` へ置き換える
- [x] 新しい `RefreshToken` 生成箇所に `user.tenantId()` を渡すよう修正する（フィールド順に合わせる）
- [x] `AuthService.java` の Javadoc を必要に応じて更新する
- [x] `AuthServiceTest.java` に `@Mock RefreshTokenExpirationPolicy refreshTokenExpirationPolicy` を追加する
- [x] `refresh_success` に `RefreshTokenExpirationPolicy` のスタブ設定と、保存された新規トークンの `tenantId()` を検証するアサーションを追加する
- [x] `validOldToken()` および `refresh_tokenRevoked` / `refresh_tokenExpired` 内の `new RefreshToken(...)` 呼び出しに `TenantId` 引数を追加する

## テストコードの記載

- [x] 正常系のテストコードを記載する（上記の各修正・追加テストを含む）
- [x] 異常系のテストコードを記載する（上記の各修正・追加テストを含む）

## TODO.md の最終更新

- [x] TODO.md の `Port (domain)` セクション `[ ] RefreshTokenExpirationPolicy#expiration` のチェックボックスは、本増分では未実装のスタブのため埋めない（空のままとする）
- [x] TODO.md の `infrastructure.security` セクション `[ ] StubRefreshTokenExpirationPolicy を実装へ置換` のチェックボックスは、本増分では未実装のスタブのため埋めない（空のままとする）
