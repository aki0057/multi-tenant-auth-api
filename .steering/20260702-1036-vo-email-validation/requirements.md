# requirements

## 作業概要
`Email`（`domain/model/vo/Email.java`）はメールアドレスを表す Value Object だが、現状は `public record Email(String value) {}` のみで検証ロジックを持たない。DB カラム `users.email VARCHAR(254) NOT NULL`（`docs/database-design.md` 準拠）に合わせ、コンパクトコンストラクタによる検証（null・空白拒否、メールアドレス形式検証、最大長 254 文字）を追加し、`TenantCode.java` と同水準の Javadoc を記載する。`LoginRequest` の `@Email` はあくまで HTTP 入力層のバリデーションであり、`Email` VO は `infrastructure.persistence.mapper.UserMapper` 経由で DB の値からも生成されるため、VO 自身が不変条件として形式検証を持つ必要がある。あわせて `EmailTest` を新規作成し、正常系・異常系を網羅する。

## 作業対象レイヤー
domain（ValueObject）

## 作業対象の種別
ValueObject

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit5 + AssertJ（`assertThat` / `assertThatThrownBy`）
- Spring MVC アノテーション: なし（domain 層のため Spring 依存なし）

## 隣接レイヤー
- 1 つ外側のレイヤー: application（`AuthService#login` が `new Email(command.email())` で生成）、domain（`User` の record コンポーネント、`UserRepository#findByTenantCodeAndEmail` の引数）、infrastructure（`UserMapper#toEmail` / `UserRepositoryImpl#findByTenantCodeAndEmail` が `Email` を生成・利用）
- 1 つ内側のレイヤー: なし（`Email` は VO であり、これより内側の呼び出し先を持たない）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる。実在の呼び出し箇所は以下（いずれも `new Email(String value)` の形で生成し、戻り値の `Email` を保持する）。

- `application/AuthService.java`（`login` メソッド内）
  - 該当箇所: `final Email email = new Email(command.email());`
  - 引数: `String`（`LoginCommand#email()`）
  - 戻り値: `Email`
- `domain/model/User.java`（record コンポーネント）: `Email email` を保持。`User` の生成時（`UserMapper#toDomain`）に `Email` インスタンスが渡される。
- `domain/repository/UserRepository.java`（インターフェース）
  - メソッド名: `findByTenantCodeAndEmail`
  - 引数: `TenantCode tenantCode, Email email`
  - 戻り値: `Optional<User>`
- `infrastructure/persistence/repository/UserRepositoryImpl.java`（`findByTenantCodeAndEmail` の実装内）
  - 該当箇所: `email.value()` を呼び出し、JPA リポジトリへ `String` として渡す。
- `infrastructure/persistence/mapper/UserMapper.java`（`toEmail` メソッド）
  - メソッド名: `toEmail`
  - 引数: `String value`
  - 戻り値: `Email`（内部で `new Email(value)` を呼び出す）

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド（`Email` のコンパクトコンストラクタ）。

- メソッド名: `Email`（コンパクトコンストラクタ）
- 引数: `String value`
- 戻り値: なし（コンストラクタ。検証失敗時は `IllegalArgumentException` をスロー、成功時は自身の `Email` インスタンスを生成）

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
該当なし（作業対象は ValueObject のため対象外）。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
該当なし（作業対象は ValueObject のため対象外）。

## 検証仕様の詳細（補足）
`TenantCode` の検証方針に倣い、以下を `Email` のコンパクトコンストラクタで検証する。

- `value` が `null` の場合: `IllegalArgumentException` をスロー。
- `value.isBlank()` が真の場合（空文字・空白のみ）: `IllegalArgumentException` をスロー。
- `value.length() > 254` の場合（DB カラム `users.email VARCHAR(254)` に合わせた上限）: `IllegalArgumentException` をスロー。
- `value` がメールアドレス形式に一致しない場合: `IllegalArgumentException` をスロー。形式判定は `jakarta.validation.constraints.Email` と同等の一般的なメールアドレス形式（`ローカル部@ドメイン部`、`@` を 1 つのみ含み、ドメイン部にドット区切りのラベルを持つ）を正規表現で検証する（実装時に具体的なパターンを決定してよいが、過度に複雑な RFC 完全準拠パターンにはしない）。
