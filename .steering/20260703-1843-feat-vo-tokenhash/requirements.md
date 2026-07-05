# requirements

## 作業概要
`TokenHash`（`domain/model/vo/TokenHash.java`）はリフレッシュトークンの SHA-256 ハッシュ値（hex 文字列）を表す Value Object だが、現状は `public record TokenHash(String value) {}` のみで検証ロジックを持たないスタブ（`// TODO: 値検証ロジックを実装（後続 domain 増分）`）である。DB カラム `refresh_tokens.token_hash VARCHAR(255) NOT NULL, UNIQUE`（備考: `SHA-256ハッシュで保存`。`docs/database-design.md` 準拠）に合わせ、コンパクトコンストラクタで以下 3 段の検証を追加する。

1. `null` 拒否
2. 空欄（`isBlank()`）拒否
3. SHA-256 hex 文字列形式チェック（正規表現 `^[0-9a-fA-F]{64}$`。SHA-256 のダイジェスト長は 32 バイト = 64 桁の hex 文字列）

実装は兄弟 VO である `Email.java` / `PasswordHash.java`（同ディレクトリ）と同じ構造・スタイル（コンパクトコンストラクタ、`private static final Pattern` 定数、Javadoc でフィールド用途と DB カラムを説明、各チェックごとに個別の `IllegalArgumentException` メッセージ、メッセージ文言パターン「〜はnullにできません。」「〜は空欄にできません。」「〜は◯◯形式である必要があります。」）に厳密に合わせる。実装前に必ずこれら 2 ファイルを読み、規約を踏襲すること。

あわせて `TokenHashTest`（`domain/model/vo/TokenHashTest.java`）を新規作成し、正常系（有効な 64 桁 hex 文字列）・異常系（null / 空欄 / 桁数不足 / 桁数超過 / hex 以外の文字混入）を網羅する。

## 付随修正（スコープ外レイヤーのテストデータ調整、機能追加なし）
`TokenHash` に SHA-256 hex 形式検証を追加すると、既存テスト `application/AuthServiceTest.java` がプレースホルダー文字列 `"old-token-hash"` / `"new-token-hash"` で `new TokenHash(...)` を呼んでいる箇所（171・173行目、`OLD_TOKEN_HASH` / `NEW_TOKEN_HASH` の定数定義）が、生成時に `IllegalArgumentException` で失敗するようになる。これは新規機能ではなく、`TokenHash` の不変条件強化に追従するための機械的なリテラル置換（テストフィクスチャの修正）である。

- `src/test/java/io/github/aki0057/multitenant/auth/application/AuthServiceTest.java`
  - 171行目: `private static final TokenHash OLD_TOKEN_HASH = new TokenHash("old-token-hash");` → 64 桁 hex 文字列（例: `"a".repeat(64)` あるいは実在しそうな hex 文字列。他の定数と区別できる値とする）へ置換
  - 173行目: `private static final TokenHash NEW_TOKEN_HASH = new TokenHash("new-token-hash");` → OLD とは異なる 64 桁 hex 文字列へ置換
  - 他のロジック・アサーション（`OLD_TOKEN_HASH` / `NEW_TOKEN_HASH` を参照する箇所）は変更しない（値が変わるだけで、比較対象は一貫して同じ定数を参照するため挙動に影響しない）

## 作業対象レイヤー
domain（ValueObject）

## 作業対象の種別
ValueObject

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 (Jupiter) + AssertJ（`assertThat` / `assertThatThrownBy`）。Mockito は不要（プレーンな JUnit、Spring 未起動）。
- Spring MVC アノテーション: 使用しない。

## 隣接レイヤー
- 1 つ外側のレイヤー: domain（`RefreshToken` の record コンポーネント `tokenHash`、`domain.repository.RefreshTokenRepository#findByTokenHash` の引数、`domain.service.RefreshTokenHasher#hash` の戻り値）
- 1 つ内側のレイヤー: なし（`TokenHash` はプリミティブ `String` をラップするのみで、これより内側のレイヤーを呼び出さない）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる（変更不可）。

- `domain/model/RefreshToken.java`（record コンポーネント、26行目）: `TokenHash tokenHash` を保持する。
- `domain/repository/RefreshTokenRepository.java`（`findByTokenHash` メソッド、21行目）
  - 引数: `TokenHash tokenHash`
  - 戻り値: `Optional<RefreshToken>`
- `domain/service/RefreshTokenHasher.java`（`hash` メソッド、19行目）
  - 引数: `RawRefreshToken rawRefreshToken`
  - 戻り値: `TokenHash`
- `application/AuthService.java`（`refresh` メソッド内、114行目）: `TokenHash tokenHash = refreshTokenHasher.hash(rawRefreshToken);` の形で `TokenHash` を受け取る。

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド（`TokenHash` のコンパクトコンストラクタ）。

- メソッド名: `TokenHash`（コンパクトコンストラクタ、`public TokenHash { ... }`）
- 引数: `String value`
- 戻り値: なし（コンストラクタ）。検証失敗時は `IllegalArgumentException` をスロー。正常時は `value` をそのまま保持した `TokenHash` インスタンスを生成する。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
該当なし（作業対象は ValueObject のため対象外）。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
該当なし（作業対象は ValueObject のため対象外）。

## 検証仕様の詳細（補足）
コンパクトコンストラクタで以下を上から順に検証する（`Email` / `PasswordHash` の検証順序パターンに倣う）。

1. `value == null` の場合: `IllegalArgumentException`（メッセージ: 「TokenHash はnullにできません。」）
2. `value.isBlank()` が真の場合: `IllegalArgumentException`（メッセージ: 「TokenHash は空欄にできません。」）
3. SHA-256 hex 形式に一致しない場合: `IllegalArgumentException`（メッセージ: 「TokenHash はSHA-256のhex文字列（64桁の16進数）である必要があります。」）
   - 正規表現: `private static final Pattern PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");`
   - 意味: 0-9 / a-f / A-F のみで構成される、ちょうど 64 桁の文字列（SHA-256 は 32 バイトのダイジェストであり、hex エンコードで 64 桁になる）。

## テスト仕様の詳細（補足）
`TokenHashTest.java`（`domain/model/vo/TokenHashTest.java`）を新規作成する。命名規則・`@DisplayName` は `docs/testing-guidelines.md` に従う（`正常系:` / `異常系:` 接頭辞、`methodName_condition()`）。

- 正常系: 有効な 64 桁 hex 文字列（例: 小文字のみ `"a".repeat(64)`、または大文字混在）を渡すと `value()` が同じ値を返すこと。
- 異常系: `null`・空文字・空白のみ・64 桁未満・64 桁超過・hex 以外の文字（`g`、記号等）を含む値を渡すと `IllegalArgumentException` がスローされること。
