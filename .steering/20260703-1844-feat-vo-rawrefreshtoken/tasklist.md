# tasklist

- [x] 作業対象（`RawRefreshToken` のコンパクトコンストラクタ）を実装する（`null` 拒否・空欄拒否のみを、`RawPassword` と同じ構造・メッセージ文言パターンで実装する。長さ・文字種チェックは行わない。クラス上部の `// TODO: 値検証ロジックを実装（後続 domain 増分）` コメントを削除する）
- [x] Javadoc を記載する（クラス Javadoc とコンパクトコンストラクタの `@param` / `@throws` を、`RawPassword` の記載パターンに合わせて記載する。緩い検証に留める理由（生成方式未確定）を記載する）
- [x] 正常系のテストコードを記載する（`RawRefreshTokenTest` を新規作成し、空欄でない文字列を渡した場合に `value()` が同じ値を返すことを検証する）
- [x] 異常系のテストコードを記載する（`RawRefreshTokenTest` に null・空欄（空文字・空白のみ）のケースを追加し、いずれも `IllegalArgumentException` がスローされることを検証する）
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックスを埋める（`## ValueObject (domain)` 章の `- [ ] RawRefreshToken` を `- [x] RawRefreshToken` にする）
