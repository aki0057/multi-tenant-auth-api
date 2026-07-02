# tasklist

- [x] 作業対象（`PasswordHash` のコンパクトコンストラクタ）を実装する（`null` 拒否・空欄拒否・255 文字超拒否・BCrypt 形式検証（`^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$`）を、`Email`/`Role`/`RawPassword` と同じ構造・定数命名・メッセージ文言パターンで実装する）
- [x] Javadoc を記載する（クラス Javadoc とコンパクトコンストラクタの `@param` / `@throws` を、`Email` / `Role` / `RawPassword` の記載パターン（フィールド用途・DB カラム説明を含む）に合わせて記載する）
- [x] 正常系のテストコードを記載する（`PasswordHashTest` を新規作成し、有効な BCrypt 形式文字列を渡した場合に `value()` が同じ値を返すことを検証する）
- [x] 異常系のテストコードを記載する（`PasswordHashTest` に null・空文字・空白のみ・255 文字超・BCrypt 形式不一致（接頭辞不正・cost 桁数不正・salt+hash 長不正等）のケースを追加し、いずれも `IllegalArgumentException` がスローされることを検証する）
- [x] 付随修正: `new PasswordHash("hashed-pass")` を実在する BCrypt 形状文字列 `$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy` に置換する（対象: `UserTest.java` / `AuthServiceTest.java` / `JwtAccessTokenProviderTest.java` / `JwtAccessTokenVerifierTest.java` / `UserMapperTest.java`（33/44/62/85/106行目の `.passwordHash("$2a$10$hashedpassword")` 呼び出しおよび対応するアサーション文字列）の計5箇所。ロジック・アサーションは変更しない）
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックスを埋める（`## ValueObject (domain)` 章の `- [ ] PasswordHash` を `- [x] PasswordHash` にする）
