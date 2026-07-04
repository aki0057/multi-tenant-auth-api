# requirements

## 作業概要
`RefreshTokenId`（`domain/model/vo/RefreshTokenId.java`）は `refresh_tokens.id`（`BIGSERIAL` 主キー、`docs/database-design.md` 準拠）を表す Value Object だが、現状は `public record RefreshTokenId(Long value) {}` のみで検証ロジックを持たないスタブ（`// TODO: 値検証ロジックを実装（後続 domain 増分）`）である。兄弟 VO である `UserId.java` / `TenantId.java`（同ディレクトリ、いずれも `BIGSERIAL` 主キーをラップする VO）と全く同じ検証（`null` 拒否・0 以下拒否）・同じ構造・同じ Javadoc パターン・同じ日本語エラーメッセージ文言（「〜はnullにできません。」「〜は1以上の整数である必要があります。」）で実装する。実装前に必ず `UserId.java` を読み、規約を踏襲すること。

あわせて `RefreshTokenIdTest`（`domain/model/vo/RefreshTokenIdTest.java`）を新規作成し、`UserIdTest` と同じ構成（正常系 1 件・異常系 3 件: null・0・負の値）で網羅する。

## 作業対象レイヤー
domain（ValueObject）

## 作業対象の種別
ValueObject

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 (Jupiter) + AssertJ（`assertThat` / `assertThatThrownBy`）。Mockito は不要（プレーンな JUnit、Spring 未起動）。
- Spring MVC アノテーション: 使用しない。

## 隣接レイヤー
- 1 つ外側のレイヤー: domain（`RefreshToken` の record コンポーネント `id`。`domain/model/RefreshToken.java`）
- 1 つ内側のレイヤー: なし（`RefreshTokenId` はプリミティブ `Long` をラップするのみで、これより内側のレイヤーを呼び出さない）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる（変更不可）。

- `domain/model/RefreshToken.java`（record コンポーネント、24行目）: `RefreshTokenId id` を保持する（未永続化の場合は `null` を許容するため、`RefreshToken` の生成時に `id` フィールドそのものへ `null` を渡すケースがある点は `RefreshTokenId` VO の検証対象外。VO の検証対象は「`new RefreshTokenId(value)` を呼び出して生成する場合の `value` 引数」のみである）
- `application/AuthServiceTest.java`（テストコードでの生成例、180行目付近）: `new RefreshTokenId(10L)` の形で生成する。

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド（`RefreshTokenId` のコンパクトコンストラクタ）。

- メソッド名: `RefreshTokenId`（コンパクトコンストラクタ、`public RefreshTokenId { ... }`）
- 引数: `Long value`
- 戻り値: なし（コンストラクタ）。検証失敗時は `IllegalArgumentException` をスロー。正常時は `value` をそのまま保持した `RefreshTokenId` インスタンスを生成する。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
該当なし（作業対象は ValueObject のため対象外）。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
該当なし（作業対象は ValueObject のため対象外）。

## 検証仕様の詳細（補足）
`UserId` / `TenantId` と全く同じ 2 段検証をこの順で行う。

1. `value == null` の場合: `IllegalArgumentException`（メッセージ: 「RefreshTokenId はnullにできません。」）
2. `value <= 0` の場合: `IllegalArgumentException`（メッセージ: 「RefreshTokenId は1以上の整数である必要があります。」）

## テスト仕様の詳細（補足）
`RefreshTokenIdTest.java`（`domain/model/vo/RefreshTokenIdTest.java`）を新規作成する。命名規則・`@DisplayName` は `docs/testing-guidelines.md` に従う（`正常系:` / `異常系:` 接頭辞、`methodName_condition()`）。`UserIdTest` と同じ 4 ケース構成とする。

- 正常系: 1 以上の正整数（例: `1L`）を渡すと `value()` が同じ値を返すこと。
- 異常系: `null` を渡すと `IllegalArgumentException` がスローされること。
- 異常系: `0` を渡すと `IllegalArgumentException` がスローされること。
- 異常系: 負の値（例: `-1L`）を渡すと `IllegalArgumentException` がスローされること。
