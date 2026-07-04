# requirements

## 作業概要

`RefreshToken`（`domain/model/RefreshToken.java`）に、公開メソッド `isValid(Instant now)`（`!revoked && !isExpired(now)` を返す）を追加し、「リフレッシュトークンが使用可能か」というドメイン知識の定義を `RefreshToken` へ一元化する。

具体的には次の変更を行う。

1. `RefreshToken` に `public boolean isValid(Instant now)` を追加する（`!revoked && !isExpired(now)` を返す。例外はスローしない）。
2. 既存の `isRevoked()` / `isExpired(Instant)` / `revoke()` の実装・シグネチャは変更しない。

`application.AuthService#refresh` が現在インラインで行っている判定 `if (oldToken.isRevoked() || oldToken.isExpired(now))` を `if (!oldToken.isValid(now))` に置き換える作業は、本増分では行わない（`AuthService` は application 層であり本作業対象の外側のため、追随修正は別の steering ディレクトリで行う）。本増分で追加する `isValid` は、その置き換えに使われることを前提に設計する。

参考: `User#isActive()`（コミット `26d1d80`）と同じパターン（複数の判定条件を単一の boolean 判定メソッドへ集約し、呼び出し元の分岐を単純化する）。

---

## 作業対象レイヤー

domain（DomainObject）

## 作業対象の種別

DomainObject

---

## 使用するテスト・フレームワーク等

- テストフレームワーク: JUnit 5 (Jupiter) + AssertJ（`assertThat`）。`RefreshTokenTest` は既存どおりプレーンな JUnit のみ（Spring コンテキスト不要・モックなし）。
- Spring MVC アノテーション: なし（domain 層のみで presentation 層の変更なし）

---

## 隣接レイヤー

- 1 つ外側のレイヤー: application（`AuthService#refresh`）
- 1 つ内側のレイヤー: なし（`isValid` は自身が保持する既存フィールド `revoked` と既存メソッド `isExpired(Instant)` のみを参照する純粋なロジックであり、内側レイヤー（Repository・ドメインポート等）を新たに呼び出さない）

---

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)

外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる。

- `application/AuthService.java`（`refresh` メソッド）
  - メソッド名: `refresh`
  - 引数: `@NonNull RefreshCommand command`
  - 戻り値: `RefreshResult`
  - 補足: 現状は `RefreshToken` に対して `oldToken.isRevoked()` / `oldToken.isExpired(now)` を個別に呼び出し、`if (oldToken.isRevoked() || oldToken.isExpired(now))` という判定を行っている。本増分は `AuthService` 自体を変更しないが、後続の別 steering でこの判定が `if (!oldToken.isValid(now))` へ置き換えられる前提でメソッドを設計する。

## 作業対象メソッドのシグネチャ

作業概要を達成するための単一メソッド。

- メソッド名: `isValid`
- 引数: `Instant now`
- 戻り値: `boolean`（`!revoked && !isExpired(now)` を返す。失効済みでなく、かつ期限切れでない場合に `true`。例外はスローしない）

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)

該当なし。判定早見表上は DomainObject 種別のため ✓ と機械的に判定されるが、`isValid` は自身が保持する既存フィールド（`revoked`）と既存メソッド（`isExpired(Instant)`、内部で `expiresAt` を参照）のみを用いた純粋なロジックであり、新たな Repository・ドメインポート・スタブを必要としない。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)

プリミティブ型への依存を禁止する方針だが、本タスクは新規のプリミティブ依存を追加しない。引数 `Instant now` は既存の `isExpired(Instant now)` と同一の型であり、本プロジェクトでは `Instant` を DomainObject の日時表現としてそのまま使用する既存方針（`RefreshToken#expiresAt` も `Instant` 型）に倣う。判定早見表上は DomainObject 種別のため「DomainObject/VO を作成」が ✓ と機械的に判定されるが、本増分で新規に作成する DomainObject / ValueObject はない。

- 作成する DomainObject / ValueObject 名: なし（新規作成なし。既存の `RefreshToken`（record）に instance method を 1 つ追加するのみ）

---

## テスト仕様の詳細（補足）

既存の `RefreshTokenTest.java`（`domain/model/RefreshTokenTest.java`）に `isValid(Instant)` のテストを追記する。命名規則・`@DisplayName` は `docs/testing-guidelines.md` に従う（`正常系:` / `異常系:` 接頭辞、`methodName_condition()`）。既存のヘルパー `newToken(Instant expiresAt, boolean revoked)` を再利用する。

- 正常系: `isValid(now)` — 未失効（`revoked=false`）かつ未期限切れ（`expiresAt` が `now` より未来）の場合は `true` を返すこと。
- 異常系: `isValid(now)` — 失効済み（`revoked=true`）の場合は（期限切れでなくても）`false` を返すこと。
- 異常系: `isValid(now)` — 期限切れ（`expiresAt` が `now` より過去）の場合は（未失効でも）`false` を返すこと。

## 変更対象ファイル一覧（本タスクが操作するすべてのファイル）

| 操作 | ファイルパス（`src/` 以下） | 内容 |
|----|-----------------|----|
| 変更 | `main/.../domain/model/RefreshToken.java` | `public boolean isValid(Instant now)`（`!revoked && !isExpired(now)` を返す）を追加し、Javadoc を記載 |
| 変更 | `test/.../domain/model/RefreshTokenTest.java` | `isValid(Instant)` の正常系・異常系テストを追記 |
