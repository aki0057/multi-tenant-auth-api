# tasklist

- [x] 作業対象を実装する（`Role` にコンパクトコンストラクタを追加し、null・空白・許容値ホワイトリスト（`ADMIN`/`USER`）・最大長 20 文字の検証を行う）
- [x] Javadoc を記載する（`TenantCode.java` と同水準の具体性で、クラス Javadoc・コンストラクタ Javadoc・`@param`/`@throws` を記載する）
- [x] 正常系のテストコードを記載する（`RoleTest` を新規作成し、許容値 `ADMIN` / `USER` を渡した場合に `value()` が一致することを検証する）
- [x] 異常系のテストコードを記載する（`RoleTest` に null・空文字・空白のみ・許容外の値・20 文字超のケースを追加し、いずれも `IllegalArgumentException` がスローされることを検証する）
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックスを埋める（`## ValueObject (domain)` 章の `- [ ] Role` を `- [x] Role` にする）
