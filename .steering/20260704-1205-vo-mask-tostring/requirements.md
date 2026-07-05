# requirements

## 作業概要

既存の ValueObject である `RawRefreshToken`（`domain/model/vo/RawRefreshToken.java`）と `RawPassword`（`domain/model/vo/RawPassword.java`）に、それぞれ `toString()` をオーバーライドし、値をマスキングした固定文字列を返すようにする。

現状の両クラスは `record` の自動生成 `toString()`（例: `RawRefreshToken[value=xxxxx]` / `RawPassword[value=xxxxx]`）をそのまま使用しており、生のリフレッシュトークン・平文パスワードという機密値がログ出力や例外メッセージ、デバッガ表示などを通じてそのまま流出するリスクがある。本タスクはこれを防ぐため、`toString()` をオーバーライドして値を含まない固定文字列（マスキング表現）を返すようにする。

- `RawRefreshToken#toString()` → 値を含まない固定文字列を返す（例: `"RawRefreshToken[masked]"`）。
- `RawPassword#toString()` → 値を含まない固定文字列を返す（例: `"RawPassword[masked]"`）。

**テスト方針（ユーザー指示による限定）**: テストは「`toString()` の戻り値が、生成に使った元の `String` と等しくないこと」を検証する正常系テストのみとする。異常系テストは本作業では対象外とする。理由: `toString()` は入力値に応じた分岐や検証を行わず、常に同一の固定文字列を返すだけの単純なメソッドであり、例外をスローする経路が存在しないため、異常系という概念自体が成立しない。加えてユーザーからテスト範囲を明示的に限定する指示を受けている。

本タスクは `RawRefreshToken` と `RawPassword` の 2 クラスを対象とするが、いずれも同一レイヤー（domain）・同一種別（ValueObject）に属し、同じ変更パターン（`toString()` のマスキングオーバーライド）を適用するだけの対称的な作業であるため、1 つの steering にまとめる。

## 作業対象レイヤー
domain（ValueObject）

## 作業対象の種別
ValueObject

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 (Jupiter) + AssertJ（`assertThat`）。Mockito は不要（プレーンな JUnit、Spring 未起動）。
- Spring MVC アノテーション: 使用しない。

## 隣接レイヤー
- 1 つ外側のレイヤー: `toString()` は `java.lang.Object` から継承される既定メソッドであり、特定の外側レイヤーのクラスが明示的に呼び出す設計にはなっていない。現時点でコードベース内に `RawRefreshToken#toString()` / `RawPassword#toString()` を明示的に呼び出している箇所は存在しない（文字列結合・ログ出力・デバッガ表示・例外メッセージ生成など、`Object` を継承するあらゆるクラスに対して暗黙的に呼び出されうる）。なお、両 VO 自体は既存の外側レイヤー（`application/AuthService.java`、`domain/model/User.java`、`domain/service/PasswordVerifier.java`、`domain/service/RefreshTokenGenerator.java`、`domain/service/RefreshTokenHasher.java`、`infrastructure/security/PasswordEncoderVerifier.java`、`infrastructure/security/StubRefreshTokenGenerator.java`、`infrastructure/security/StubRefreshTokenHasher.java`）から値の受け渡しに利用されているが、これらは `toString()` を呼ばないため今回のシグネチャ変更（追加）による影響はない。
- 1 つ内側のレイヤー: なし（`toString()` はインスタンスが保持する固定文字列を返すのみで、内側レイヤーを呼び出さない）。

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
`toString()` は `java.lang.Object` のオーバーライドであり、シグネチャは Java 言語仕様上固定されている（変更不可）。既存の外側レイヤーで本メソッドを明示的に呼び出している箇所はないが、契約として次のシグネチャに合わせる。
- メソッド名: `toString`
- 引数: なし
- 戻り値: `String`

## 作業対象メソッドのシグネチャ
作業概要を達成するための対称的な 2 メソッド（`RawRefreshToken` と `RawPassword` それぞれに 1 つずつ）。

`RawRefreshToken`:
- メソッド名: `toString`
- 引数: なし
- 戻り値: `String`（値を含まないマスキング済み固定文字列。例: `"RawRefreshToken[masked]"`。例外はスローしない）

`RawPassword`:
- メソッド名: `toString`
- 引数: なし
- 戻り値: `String`（値を含まないマスキング済み固定文字列。例: `"RawPassword[masked]"`。例外はスローしない）

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
（該当なし。作業対象は ValueObject のため本項目は対象外。）

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
（該当なし。作業対象は ValueObject のため本項目は対象外。）

## 変更対象ファイル一覧（本タスクが操作するすべてのファイル）

| 操作 | ファイルパス（`src/` 以下） | 内容 |
|----|-----------------|----|
| 変更 | `main/.../domain/model/vo/RawRefreshToken.java` | `toString()` をオーバーライドし、値を含まないマスキング済み固定文字列を返すようにする |
| 変更 | `main/.../domain/model/vo/RawPassword.java` | `toString()` をオーバーライドし、値を含まないマスキング済み固定文字列を返すようにする |
| 変更 | `test/.../domain/model/vo/RawRefreshTokenTest.java` | 既存テストクラスに `toString()` の正常系テストを追記（戻り値が元の生成用文字列と等しくないことのみを検証） |
| 変更 | `test/.../domain/model/vo/RawPasswordTest.java` | 既存テストクラスに `toString()` の正常系テストを追記（戻り値が元の生成用文字列と等しくないことのみを検証） |
