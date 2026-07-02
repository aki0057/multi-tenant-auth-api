# tasklist

- [x] 作業対象を実装する（`Email` にコンパクトコンストラクタを追加し、null・空白・メールアドレス形式・最大長 254 文字の検証を行う）
- [x] Javadoc を記載する（`TenantCode.java` と同水準の具体性で、クラス Javadoc・コンストラクタ Javadoc・`@param`/`@throws` を記載する）
- [x] 正常系のテストコードを記載する（`EmailTest` を新規作成し、有効なメールアドレス形式を渡した場合に `value()` が一致することを検証する。254 文字ちょうどの境界値も含む）
- [x] 異常系のテストコードを記載する（`EmailTest` に null・空文字・空白のみ・不正な形式（`@` なし・`@` 複数・ドメイン部欠如など）・254 文字超のケースを追加し、いずれも `IllegalArgumentException` がスローされることを検証する）
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックスを埋める（`## ValueObject (domain)` 章の `- [ ] Email` を `- [x] Email` にする）
