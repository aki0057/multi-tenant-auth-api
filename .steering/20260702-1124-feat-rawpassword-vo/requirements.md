# requirements

## 作業概要
`RawPassword` Value Object（`domain/model/vo/RawPassword.java`）に検証ロジックを実装する。現状は `public record RawPassword(String value) {}` のみで検証が未定義のため、以下の条件で正規化コンパクトコンストラクタを実装し、無効値の場合は `IllegalArgumentException` をスローする。
- `null` を拒否する
- 空白のみ（空文字含む）を拒否する
- 8 文字以上であることを要求する
- 半角英数字（`A-Za-z0-9`）のみで構成されることを要求する

あわせて `RawPasswordTest`（`domain/model/vo/RawPasswordTest.java`）を新規作成し、正常系・異常系のテストを追加する。

## 作業対象レイヤー
domain（ValueObject）

## 作業対象の種別
ValueObject

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 (Jupiter) + AssertJ（`assertThat` / `assertThatThrownBy`）。Mockito は不要（プレーンな JUnit、Spring 未起動）。
- Spring MVC アノテーション: 使用しない。

## 隣接レイヤー
- 1 つ外側のレイヤー: application（`AuthService`）および domain（`User#authenticate`, `PasswordVerifier`）、infrastructure.security（`PasswordEncoderVerifier`）。いずれも既存コードから `RawPassword` を利用している。
- 1 つ内側のレイヤー: なし（`RawPassword` はプリミティブ `String` をラップするのみで、これより内側のレイヤーを呼び出さない）。

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる。`RawPassword` は record のコンパクトコンストラクタとして検証を実装するため、外側から見た契約はコンストラクタのシグネチャそのものになる。既存の呼び出し箇所は以下の通り（シグネチャ変更不可）。
- メソッド名: `RawPassword`（正規コンストラクタ）
- 引数: `String value`
- 戻り値: `RawPassword`（`value` フィールドを保持する record インスタンス。無効値の場合は `IllegalArgumentException` をスロー）

既存の呼び出し箇所（参考、変更しない）:
- `application/AuthService.java:42` — `new RawPassword(command.password())`
- `domain/model/User.java:31` — `authenticate(RawPassword rawPassword, PasswordVerifier passwordVerifier)` の引数
- `domain/service/PasswordVerifier.java:20` — `matches(RawPassword rawPassword, PasswordHash passwordHash)` の引数
- `infrastructure/security/PasswordEncoderVerifier.java:28` — `matches(RawPassword rawPassword, PasswordHash passwordHash)` の引数
- `test/.../UserTest.java:14` — `new RawPassword("password")`（8 文字・半角英字のみで新検証ルールに適合、変更不要）

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド（record のコンパクトコンストラクタ）。
- メソッド名: `RawPassword`（コンパクトコンストラクタ、`public RawPassword { ... }`）
- 引数: `String value`
- 戻り値: なし（コンストラクタ）。検証失敗時は `IllegalArgumentException` をスロー。正常時は `value` をそのまま保持した `RawPassword` インスタンスを生成する。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
（該当なし。作業対象は ValueObject のため本項目は対象外。）

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
（該当なし。作業対象は ValueObject のため本項目は対象外。）
