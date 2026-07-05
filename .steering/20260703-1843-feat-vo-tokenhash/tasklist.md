# tasklist

- [x] 作業対象（`TokenHash` のコンパクトコンストラクタ）を実装する（`null` 拒否・空欄拒否・SHA-256 hex 形式検証（`^[0-9a-fA-F]{64}$`）を、`Email`/`PasswordHash` と同じ構造・定数命名・メッセージ文言パターンで実装し、クラス上部の `// TODO: 値検証ロジックを実装（後続 domain 増分）` コメントを削除する）
- [x] Javadoc を記載する（クラス Javadoc とコンパクトコンストラクタの `@param` / `@throws` を、`Email` / `PasswordHash` の記載パターンに合わせて記載する）
- [x] 正常系のテストコードを記載する（`TokenHashTest` を新規作成し、有効な 64 桁 hex 文字列を渡した場合に `value()` が同じ値を返すことを検証する）
- [x] 異常系のテストコードを記載する（`TokenHashTest` に null・空文字・空白のみ・64 桁未満・64 桁超過・hex 以外の文字混入のケースを追加し、いずれも `IllegalArgumentException` がスローされることを検証する）
- [x] 付随修正: `AuthServiceTest.java` の `OLD_TOKEN_HASH` / `NEW_TOKEN_HASH`（171・173行目）を有効な 64 桁 hex 文字列へ置換する（ロジック・アサーションは変更しない）
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックスを埋める（`## ValueObject (domain)` 章の `- [ ] TokenHash` を `- [x] TokenHash` にする）
