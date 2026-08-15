# requirements

## 作業概要
`RawRefreshToken`（`domain/model/vo/RawRefreshToken.java`）はクライアントへ渡す生のリフレッシュトークン文字列を表す Value Object だが、現状は `public record RawRefreshToken(String value) {}` のみで検証ロジックを持たないスタブ（`// TODO: 値検証ロジックを実装（後続 domain 増分）`）である。

生成方式（`RefreshTokenGenerator` の infrastructure 実装）が未確定であり文字列の具体的な形式（長さ・文字種）を今の時点で固定できないため、本増分では緩い検証（`null` 拒否・空欄拒否のみ）に留める。形式チェック（長さ・エンコーディング等）は infrastructure 実装が確定した後続増分で追加する。`RawPassword.java`（同ディレクトリ）と同じ構造・Javadoc パターン・メッセージ文言（「〜はnullにできません。」「〜は空欄にできません。」）に合わせるが、検証段数は `RawPassword` より少ない 2 段（長さ・文字種チェックは行わない）とする。

`toString()` のマスキング（`RawPassword` を含め、生の機密文字列を `toString()` 出力から隠す対応）はユーザー決定により本増分のスコープ外とする。実装しないこと。

あわせて `RawRefreshTokenTest`（`domain/model/vo/RawRefreshTokenTest.java`）を新規作成し、正常系 1 件・異常系 2 件（null・空欄）を網羅する。

## 作業対象レイヤー
domain（ValueObject）

## 作業対象の種別
ValueObject

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 (Jupiter) + AssertJ（`assertThat` / `assertThatThrownBy`）。Mockito は不要（プレーンな JUnit、Spring 未起動）。
- Spring MVC アノテーション: 使用しない。

## 隣接レイヤー
- 1 つ外側のレイヤー: domain（`domain.service.RefreshTokenGenerator#generate` の戻り値、`domain.service.RefreshTokenHasher#hash` の引数）、application（`AuthService#refresh` が `new RawRefreshToken(command.refreshToken())` で生成する）
- 1 つ内側のレイヤー: なし（`RawRefreshToken` はプリミティブ `String` をラップするのみで、これより内側のレイヤーを呼び出さない）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる（変更不可）。

- `application/AuthService.java`（`refresh` メソッド内、113行目）: `RawRefreshToken rawRefreshToken = new RawRefreshToken(command.refreshToken());` の形で生成する（引数は `RefreshCommand#refreshToken()` が保持する `String`）。
- `domain/service/RefreshTokenGenerator.java`（`generate` メソッド、17行目）
  - 引数: なし
  - 戻り値: `RawRefreshToken`
- `domain/service/RefreshTokenHasher.java`（`hash` メソッド、19行目）
  - 引数: `RawRefreshToken rawRefreshToken`
  - 戻り値: `TokenHash`

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド（`RawRefreshToken` のコンパクトコンストラクタ）。

- メソッド名: `RawRefreshToken`（コンパクトコンストラクタ、`public RawRefreshToken { ... }`）
- 引数: `String value`
- 戻り値: なし（コンストラクタ）。検証失敗時は `IllegalArgumentException` をスロー。正常時は `value` をそのまま保持した `RawRefreshToken` インスタンスを生成する。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
該当なし（作業対象は ValueObject のため対象外）。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
該当なし（作業対象は ValueObject のため対象外）。

## 検証仕様の詳細（補足）
`RawPassword` の検証順序パターンに倣うが、長さ・文字種チェックは行わない（生成方式未確定のため）。

1. `value == null` の場合: `IllegalArgumentException`（メッセージ: 「RawRefreshToken はnullにできません。」）
2. `value.isBlank()` が真の場合: `IllegalArgumentException`（メッセージ: 「RawRefreshToken は空欄にできません。」）

## テスト仕様の詳細（補足）
`RawRefreshTokenTest.java`（`domain/model/vo/RawRefreshTokenTest.java`）を新規作成する。命名規則・`@DisplayName` は `docs/testing-guidelines.md` に従う（`正常系:` / `異常系:` 接頭辞、`methodName_condition()`）。

- 正常系: 空欄でない任意の文字列（例: `"raw-refresh-token-value"`）を渡すと `value()` が同じ値を返すこと。
- 異常系: `null` を渡すと `IllegalArgumentException` がスローされること。
- 異常系: 空欄（空文字または空白のみ）を渡すと `IllegalArgumentException` がスローされること。
