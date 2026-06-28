# requirements

## 作業概要
`UserMapper#toDomain(UserJpaEntity)` に、User ドメインレコードへ追加された 3 フィールドのマッピングを追加する。
- `tenant.id`（Long）→ `tenantId`（TenantId）: 新規変換ヘルパー `toTenantId(Long)` を同インターフェースへ追加し、`@Mapping(source = "tenant.id", target = "tenantId")` で適用する。
- `active`（boolean）→ `userIdIsActive`（boolean）: `@Mapping(source = "active", target = "userIdIsActive")` を追加する。
- `tenant.active`（boolean）→ `tenantIdIsActive`（boolean）: `@Mapping(source = "tenant.active", target = "tenantIdIsActive")` を追加する。

これにより、MapStruct 生成実装で `tenantId=null`、`userIdIsActive=false`、`tenantIdIsActive=false` となる不具合を修正し、`LoginIntegrationTest.login_withValidCredentials_returns200` を green にする。

## 作業対象レイヤー
infrastructure（persistence/mapper）

## 作業対象の種別
infrastructure.mapper

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5（MapStruct 生成クラス `UserMapperImpl` を直接インスタンス化する単体テスト）
- Spring MVC アノテーション: なし

## 隣接レイヤー
- 1 つ外側のレイヤー: infrastructure.persistence.repository（UserRepositoryImpl）
- 1 つ内側のレイヤー: なし（UserMapper はアーキテクチャ最内層）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる。

`UserRepositoryImpl#findByTenantCodeAndEmail` が `userMapper::toDomain` をメソッド参照で呼び出す。

- メソッド名: `findByTenantCodeAndEmail`
- 引数: `TenantCode tenantCode, Email email`
- 戻り値: `Optional<User>`

（上記外側メソッド内での toDomain の呼び出し箇所: `.map(userMapper::toDomain)`）

## 作業対象メソッドのシグネチャ
作業概要を達成するための修正対象メソッドと追加ヘルパー。

### 修正対象
- メソッド名: `toDomain`
- 引数: `UserJpaEntity entity`
- 戻り値: `User`

### 追加ヘルパー（同インターフェース内 default メソッド）
- メソッド名: `toTenantId`
- 引数: `Long value`
- 戻り値: `TenantId`

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
UserMapper はアーキテクチャ最内層のため、外部コンポーネントへ委譲する内側レイヤーは存在しない。
`toTenantId(Long)` は同じインターフェース内の変換ヘルパーであり、新規スタブ作成・TODO.md への登録は不要。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper のいずれか)
プリミティブ型への依存を禁止する。必ず DomainObject または ValueObject を作成または既存のものを利用する。

`toTenantId(Long)` は `TenantId`（`domain/model/vo/TenantId.java`）を使用する。
`TenantId` は既に実装済みであるため、新規作成しない。

- 作成する DomainObject / ValueObject 名: なし（既存 `TenantId` を利用）
