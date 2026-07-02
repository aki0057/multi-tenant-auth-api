# requirements

## 作業概要

domain 層に `TenantId` ValueObject を新規実装する。
テナントの主キー（DB の `tenants.id`、型 `Long`）をラップするレコードクラスであり、
infrastructure.mapper が JPA エンティティをドメインモデルへ変換する際に使用する。
コンパクトコンストラクタでバリデーションを行い、不正値は `IllegalArgumentException` をスローする。

## 作業対象レイヤー

domain (ValueObject)

## 作業対象の種別

ValueObject

## 使用するテスト・フレームワーク等

- テストフレームワーク: JUnit 5（単体テスト。Mockito は不要）
- Spring MVC アノテーション: 不使用

## 隣接レイヤー

- 1 つ外側のレイヤー: infrastructure.mapper（UserMapper）
- 1 つ内側のレイヤー: なし（ValueObject は最内側であり、内側に依存するレイヤーは存在しない）

## 外側レイヤーとの契約

外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる。

- メソッド名: `toDomain`
- 引数: `UserJpaEntity entity`
- 戻り値: `User`
- 補足: `UserMapper#toDomain` は `entity.getTenant().getId()`（型 `Long`）を介して TenantId を構築する。
  したがって TenantId は `Long` を受け取るコンストラクタを持つ必要がある。
  実在クラス: `io.github.aki0057.multitenant.auth.infrastructure.persistence.mapper.UserMapper`

## 作業対象メソッドのシグネチャ

作業概要を達成するための単一の型定義（record コンパクトコンストラクタ）。

- メソッド名: `TenantId`（Java record のコンパクトコンストラクタ）
- 引数: `Long value`
- 戻り値: `TenantId` インスタンス
- 配置先: `io.github.aki0057.multitenant.auth.domain.model.vo.TenantId`

## バリデーション要件

コンパクトコンストラクタ内で以下を検証し、違反時は `IllegalArgumentException` をスローする。

| 条件 | 判定根拠 | スロー |
|------|---------|--------|
| `value == null` | null は DB 主キーとして無効 | `IllegalArgumentException` |
| `value <= 0` | DB の BIGSERIAL 主キーは 1 以上の正整数。0 以下は「空欄」相当の無効値とみなす | `IllegalArgumentException` |

> 補足: `Long` 型には文字列における「空欄（blank）」の概念がないため、`value <= 0` をその代替とする。
