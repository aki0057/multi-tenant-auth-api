# tasklist

- [x] TenantId が TODO.md に存在しないため、プロジェクトルートの TODO.md の「ValueObject (domain)」章に `- [ ] TenantId` を空のチェックボックスで追記する
- [x] `TenantId` を実装する（配置先: `domain/model/vo/TenantId.java`。コンパクトコンストラクタで `value == null` および `value <= 0` を検証し `IllegalArgumentException` をスロー）
- [x] Javadoc を記載する
- [x] 正常系のテストコードを記載する（例: `new TenantId(1L).value()` が `1L` を返す）
- [x] 異常系のテストコードを記載する（例: `null` 渡し → `IllegalArgumentException`、`0` 渡し → `IllegalArgumentException`、`-1` 渡し → `IllegalArgumentException`）
- [x] 今回実装した `TenantId` に対応する TODO.md のチェックボックスを埋める（完了にする）
