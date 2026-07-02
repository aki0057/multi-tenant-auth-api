# requirements

## 作業概要
`Role`（`domain/model/vo/Role.java`）はユーザーロールを表す Value Object だが、現状は `public record Role(String value) {}` のみで検証ロジックを持たない。DB カラム `users.role VARCHAR(20) NOT NULL`（許容値 `ADMIN` / `USER`。`docs/database-design.md` 準拠）に合わせ、コンパクトコンストラクタによる検証（null・空白拒否、許容値ホワイトリスト、最大長 20 文字）を追加し、`TenantCode.java` と同水準の Javadoc を記載する。あわせて `RoleTest` を新規作成し、正常系・異常系を網羅する。

## 作業対象レイヤー
domain（ValueObject）

## 作業対象の種別
ValueObject

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit5 + AssertJ（`assertThat` / `assertThatThrownBy`）
- Spring MVC アノテーション: なし（domain 層のため Spring 依存なし）

## 隣接レイヤー
- 1 つ外側のレイヤー: domain（`User` / `AuthenticatedUser` などの DomainObject。`Role role` を record コンポーネントとして保持する）、infrastructure（`UserMapper#toRole` / `JwtAccessTokenVerifier` が `new Role(String)` で生成する）
- 1 つ内側のレイヤー: なし（`Role` は VO であり、これより内側の呼び出し先を持たない）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる。実在の呼び出し箇所は以下（いずれも `new Role(String value)` の形で生成し、戻り値の `Role` を保持する）。

- `domain/model/User.java`（record コンポーネント）: `Role role` を保持。`User` の生成時（例: `infrastructure.persistence.mapper.UserMapper#toDomain`）に `Role` インスタンスが渡される。
- `domain/model/AuthenticatedUser.java`（record コンポーネント）: `Role role` を保持。JWT クレームから復元される軽量な認証情報キャリア。
- `infrastructure/persistence/mapper/UserMapper.java`（`toRole` メソッド）
  - メソッド名: `toRole`
  - 引数: `String value`
  - 戻り値: `Role`（内部で `new Role(value)` を呼び出す）
- `infrastructure/security/JwtAccessTokenVerifier.java`
  - 該当箇所: `Role role = new Role(claims.get("role", String.class));`
  - 引数: `String`（JWT の `role` クレーム値）
  - 戻り値: `Role`

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド（`Role` のコンパクトコンストラクタ）。

- メソッド名: `Role`（コンパクトコンストラクタ）
- 引数: `String value`
- 戻り値: なし（コンストラクタ。検証失敗時は `IllegalArgumentException` をスロー、成功時は自身の `Role` インスタンスを生成）

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
該当なし（作業対象は ValueObject のため対象外）。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
該当なし（作業対象は ValueObject のため対象外）。

## 検証仕様の詳細（補足）
`TenantCode` の検証方針に倣い、以下を `Role` のコンパクトコンストラクタで検証する。

- `value` が `null` の場合: `IllegalArgumentException` をスロー。
- `value.isBlank()` が真の場合（空文字・空白のみ）: `IllegalArgumentException` をスロー。
- `value.length() > 20` の場合（DB カラム `users.role VARCHAR(20)` に合わせた上限）: `IllegalArgumentException` をスロー。
- `value` が `"ADMIN"` / `"USER"` のいずれでもない場合（`docs/database-design.md` の `users.role` 備考欄に準拠したホワイトリスト）: `IllegalArgumentException` をスロー。
