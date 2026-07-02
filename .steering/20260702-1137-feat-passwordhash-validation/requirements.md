# requirements

## 作業概要
`PasswordHash`（`domain/model/vo/PasswordHash.java`）は BCrypt ハッシュ済みパスワードを表す Value Object だが、現状は `public record PasswordHash(String value) {}` のみで検証ロジックを持たない。DB カラム `users.password_hash VARCHAR(255) NOT NULL`（備考: `BCryptハッシュ`。`docs/database-design.md` 準拠）に合わせ、コンパクトコンストラクタで以下 4 段の検証を追加する。

1. `null` 拒否
2. 空欄（`isBlank()`）拒否
3. 255 文字超拒否（DB の `VARCHAR(255)` に合わせた上限）
4. BCrypt 形式チェック（正規表現 `^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$`。接頭辞 `$2a$` / `$2b$` / `$2y$`、cost 2 桁、salt+hash 53 文字、計 60 文字）

実装は兄弟 VO である `Email.java` / `Role.java` / `RawPassword.java`（同ディレクトリ）と同じ構造・スタイル（コンパクトコンストラクタ、`private static final` 定数、Javadoc でフィールド用途と DB カラムを説明、各チェックごとに個別の `IllegalArgumentException` メッセージ、メッセージ文言パターン「〜はnullにできません。」「〜は空欄にできません。」等）に厳密に合わせる。実装前に必ずこれら 3 ファイルを読み、規約を踏襲すること。

あわせて `PasswordHashTest`（`domain/model/vo/PasswordHashTest.java`）を新規作成し、正常系（有効な BCrypt 形式）・異常系（null / 空欄 / 255 文字超 / BCrypt 形式不一致）を網羅する。

## 付随修正（スコープ外レイヤーのテストデータ調整、機能追加なし）
`PasswordHash` に BCrypt 形式検証を追加すると、既存テストが非 BCrypt 形式のプレースホルダー文字列 `"hashed-pass"` で `new PasswordHash("hashed-pass")` を呼んでいる箇所が、生成時に `IllegalArgumentException` で失敗するようになる。これは新規機能ではなく、`PasswordHash` の不変条件強化に追従するための機械的なリテラル置換（テストフィクスチャの修正）である。対象は以下 5 ファイルの `new PasswordHash("hashed-pass")` 呼び出し箇所、および `PasswordHash` VO のコンストラクタを経由するテストデータ文字列のみ。置換先は `init.sql` のシード値と同一の実在する BCrypt 形状文字列 `$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy` に統一する（他のロジック・アサーションは変更しない）。

- `src/test/java/io/github/aki0057/multitenant/auth/domain/model/UserTest.java`（28行目付近）
- `src/test/java/io/github/aki0057/multitenant/auth/application/AuthServiceTest.java`（53行目付近）
- `src/test/java/io/github/aki0057/multitenant/auth/infrastructure/security/JwtAccessTokenProviderTest.java`（52行目付近）
- `src/test/java/io/github/aki0057/multitenant/auth/infrastructure/security/JwtAccessTokenVerifierTest.java`（63行目付近）
- `src/test/java/io/github/aki0057/multitenant/auth/infrastructure/persistence/mapper/UserMapperTest.java`（33/62/85/106行目の `.passwordHash("$2a$10$hashedpassword")` 呼び出し計4箇所、および44行目の対応するアサーション文字列 `assertThat(user.passwordHash().value()).isEqualTo("$2a$10$hashedpassword")`。`userMapper.toDomain(entity)` 経由で `PasswordHash` VO のコンストラクタを通るため対象に含める）

`infrastructure/persistence/repository/UserJpaRepositoryTest.java` の `.passwordHash("hashed-password")` は JPA エンティティ（`UserJpaEntity`）のビルダー呼び出しであり `PasswordHash` VO のコンストラクタを経由しないため対象外（変更しない）。

## 作業対象レイヤー
domain（ValueObject）

## 作業対象の種別
ValueObject

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 (Jupiter) + AssertJ（`assertThat` / `assertThatThrownBy`）。Mockito は不要（プレーンな JUnit、Spring 未起動）。
- Spring MVC アノテーション: 使用しない。

## 隣接レイヤー
- 1 つ外側のレイヤー: domain（`User` の record コンポーネント、`domain.service.PasswordVerifier#matches` の引数）、infrastructure.security（`PasswordEncoderVerifier#matches` の引数）、infrastructure.mapper（`UserMapper#toPasswordHash` が `new PasswordHash(value)` で生成）
- 1 つ内側のレイヤー: なし（`PasswordHash` はプリミティブ `String` をラップするのみで、これより内側のレイヤーを呼び出さない）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる（変更不可）。

- `infrastructure/persistence/mapper/UserMapper.java`（`toPasswordHash` メソッド、37行目）
  - 該当箇所: `default PasswordHash toPasswordHash(String value){ return new PasswordHash(value); }`
  - 引数: `String value`
  - 戻り値: `PasswordHash`（内部で `new PasswordHash(value)` を呼び出す）
- `domain/model/User.java`（record コンポーネント、16行目）: `PasswordHash passwordHash` を保持。`User` の生成時（`UserMapper#toDomain`）に `PasswordHash` インスタンスが渡される。
- `domain/service/PasswordVerifier.java`（インターフェース、20行目）
  - メソッド名: `matches`
  - 引数: `RawPassword rawPassword, PasswordHash passwordHash`
  - 戻り値: `boolean`
- `infrastructure/security/PasswordEncoderVerifier.java`（`matches` 実装、28行目）: `PasswordHash passwordHash` を引数として受け取り `passwordHash.value()` を利用する。

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド（`PasswordHash` のコンパクトコンストラクタ）。

- メソッド名: `PasswordHash`（コンパクトコンストラクタ、`public PasswordHash { ... }`）
- 引数: `String value`
- 戻り値: なし（コンストラクタ）。検証失敗時は `IllegalArgumentException` をスロー。正常時は `value` をそのまま保持した `PasswordHash` インスタンスを生成する。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
該当なし（作業対象は ValueObject のため対象外）。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
該当なし（作業対象は ValueObject のため対象外）。

## 検証仕様の詳細（補足）
コンパクトコンストラクタで以下を上から順に検証する（`Email` / `Role` / `RawPassword` の検証順序パターンに倣う）。

1. `value == null` の場合: `IllegalArgumentException`（メッセージ例: 「PasswordHash はnullにできません。」）
2. `value.isBlank()` が真の場合: `IllegalArgumentException`（メッセージ例: 「PasswordHash は空欄にできません。」）
3. `value.length() > 255`（`private static final int MAX_LENGTH = 255;`。DB カラム `users.password_hash VARCHAR(255)` に合わせた上限）の場合: `IllegalArgumentException`（メッセージ例: 「PasswordHash は255 文字以内である必要があります。」、`Email`/`Role` と同じ `MAX_LENGTH` 埋め込みパターン）
4. BCrypt 形式に一致しない場合: `IllegalArgumentException`（メッセージ例: 「PasswordHash はBCrypt形式である必要があります。」）
   - 正規表現: `private static final Pattern PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");`
   - 意味: 接頭辞 `$2a$` / `$2b$` / `$2y$`、コストパラメータ 2 桁、salt+hash 部 53 文字（Base64 風の `./A-Za-z0-9`）、全体で 60 文字。

## テスト仕様の詳細（補足）
`PasswordHashTest.java`（`domain/model/vo/PasswordHashTest.java`）を新規作成する。命名規則・`@DisplayName` は `docs/testing-guidelines.md` に従う（`正常系:` / `異常系:` 接頭辞、`methodName_condition()`）。

- 正常系: 有効な BCrypt 形式文字列（例: `$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`）を渡すと `value()` が同じ値を返すこと。
- 異常系: `null`・空文字・空白のみ・255 文字超・BCrypt 形式に一致しない値（接頭辞不正・cost 桁数不正・salt+hash 長不正・記号混入など）を渡すと `IllegalArgumentException` がスローされること。
