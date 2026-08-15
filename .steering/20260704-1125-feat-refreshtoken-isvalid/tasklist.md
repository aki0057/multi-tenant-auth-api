# tasklist

- [x] `RefreshToken#isValid` が TODO.md の DomainObject (domain) 章に存在しないため、空のチェックボックスで `RefreshToken#isValid` を追記する
- [x] 作業対象（`RefreshToken#isValid(Instant now)`、`!revoked && !isExpired(now)` を返す）を実装する
- [x] Javadoc を記載する（`@param` / `@return` を記載し、クラス Javadoc に `isValid` への参照を追記する）
- [x] 正常系のテストコードを記載する（`RefreshTokenTest` に、未失効かつ未期限切れの場合 `isValid` が `true` を返すことを検証するテストを追加する）
- [x] 異常系のテストコードを記載する（`RefreshTokenTest` に、失効済みの場合と期限切れの場合それぞれで `isValid` が `false` を返すことを検証するテストを追加する）
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックスを埋める（`## DomainObject (domain)` 章に追記した `RefreshToken#isValid` を `- [x]` にする）
