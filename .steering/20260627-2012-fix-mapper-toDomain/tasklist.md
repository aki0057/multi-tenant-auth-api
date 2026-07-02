# tasklist

- [x] 作業対象を実装する（`@Mapping` 3件追加・`toTenantId(Long)` ヘルパー追加）
- [x] Javadoc を記載する
- [x] 正常系のテストコードを記載する（`UserMapperTest` を新規作成し、全フィールドが正しくマッピングされることを検証する）
- [x] 異常系のテストコードを記載する（`tenant.id` が null または 0 以下の場合に `TenantId` コンストラクタが `IllegalArgumentException` をスローすることを検証する）
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックスを埋める（完了にする）（注: `[x] UserMapper#toDomain(UserJpaEntity)` は既に checked のため、状態を確認して変更不要であれば そのままにする）
