# tasklist

- [x] 作業対象（`RefreshToken#isExpired(Instant now)` / `RefreshToken#isRevoked()` / `RefreshToken#revoke()`）を実装する。クラス上部の `// TODO: 期限切れ・失効判定と revoke() を実装（後続 domain 増分）` コメントを削除する
- [x] Javadoc を記載する（クラス Javadoc（スタブ注記の削除）と各メソッドの `@param` / `@return` を記載する）
- [x] 正常系のテストコードを記載する（`RefreshTokenTest` を新規作成し、`isExpired` が未期限切れで `false` を返すこと、`isRevoked` が未失効で `false` を返すこと、`revoke()` が失効済みの新インスタンスを返しフィールドを引き継ぐことを検証する）
- [x] 異常系のテストコードを記載する（`RefreshTokenTest` に、`isExpired` が期限切れで `true` を返すこと、`isRevoked` が失効済みで `true` を返すことのケースを追加する）
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックスを埋める（`## DomainObject (domain)` 章の `- [ ] RefreshToken` を `- [x] RefreshToken` にする）
