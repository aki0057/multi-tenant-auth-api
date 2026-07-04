# tasklist

- [x] 作業対象（`RefreshTokenId` のコンパクトコンストラクタ）を実装する（`null` 拒否・0 以下拒否を、`UserId`/`TenantId` と同じ構造・メッセージ文言パターンで実装し、クラス上部の `// TODO: 値検証ロジックを実装（後続 domain 増分）` コメントを削除する）
- [x] Javadoc を記載する（クラス Javadoc とコンパクトコンストラクタの `@param` / `@throws` を、`UserId` の記載パターンに合わせて記載する）
- [x] 正常系のテストコードを記載する（`RefreshTokenIdTest` を新規作成し、1 以上の正整数を渡した場合に `value()` が同じ値を返すことを検証する）
- [x] 異常系のテストコードを記載する（`RefreshTokenIdTest` に null・0・負の値のケースを追加し、いずれも `IllegalArgumentException` がスローされることを検証する）
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックスを埋める（`## ValueObject (domain)` 章の `- [ ] RefreshTokenId` を `- [x] RefreshTokenId` にする）
