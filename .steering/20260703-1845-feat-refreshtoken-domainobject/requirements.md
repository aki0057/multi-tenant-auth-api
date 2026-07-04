# requirements

## 作業概要
`RefreshToken`（`domain/model/RefreshToken.java`）はリフレッシュトークンを表す DomainObject（record）だが、現状は純粋なデータ保持のみのスタブ（`// TODO: 期限切れ・失効判定と revoke() を実装（後続 domain 増分）`）である。次の 3 メソッドを instance method として追加する。

1. `isExpired(Instant now)`: 引数 `now` が `expiresAt` 以降（`expiresAt.isBefore(now)`）であれば `true` を返す期限切れ判定。
2. `isRevoked()`: 失効済み（`revoked == true`）であれば `true` を返す失効判定（record の自動生成アクセサ `revoked()` とは別に、ドメインの意図を表す明示的な判定メソッドとして用意する）。
3. `revoke()`: 失効済み（`revoked = true`）の新しい `RefreshToken` インスタンスを返す（record のため immutable。他のフィールドは元の値をそのまま引き継ぐ）。

現在 `application.AuthService#refresh` がインラインで行っている判定 `oldToken.revoked() || oldToken.expiresAt().isBefore(now)` と、ローテーション時の `new RefreshToken(oldToken.id(), oldToken.userId(), oldToken.tokenHash(), oldToken.expiresAt(), true)` という直接生成は、本増分では変更しない（`AuthService` は application 層であり本作業対象の外側のため、追随修正は別の steering ディレクトリで行う）。ただし本増分で追加する `isExpired` / `isRevoked` / `revoke()` は、その置き換えに使われることを前提に設計する。

あわせて `RefreshTokenTest`（`domain/model/RefreshTokenTest.java`）を新規作成し、`UserTest` と同様のプレーン JUnit（Spring 未起動）で正常系・異常系を網羅する。

**前提（実施順序）**: `RefreshTokenId` / `TokenHash` / `RawRefreshToken` の 3 ValueObject 増分（別 steering）と依存関係にある。本増分のテストコードは、VO 側の検証実装が完了しているかどうかに関わらずコンパイル・実行できるよう、最初から検証済み VO の形式（有効な 64 桁 hex の `TokenHash`、1 以上の `RefreshTokenId` 等）に準拠したテストデータを用いる。

## 注意（テンプレートからの逸脱と理由）

本 steering はテンプレート・判定早見表の機械的な適用に対して以下 2 点で逸脱する。分割せず 1 増分として扱うことが妥当かどうかの最終判断はゲートA（ユーザー承認）に委ねる。

### 1. 「作業対象メソッドのシグネチャ」の単一メソッド原則からの逸脱

テンプレートは「作業概要を達成するための単一メソッド」を前提とするが、本 steering は `isExpired` / `isRevoked` / `revoke()` の 3 メソッドをまとめて扱う。これは先例 `.steering/20260628-1031-refactor-user-authenticate`（`User#authenticate` を中心に複数レイヤーへ波及したリファクタリングで、同様にテンプレート前提からの逸脱を明記した上で 1 増分にまとめた事例）に倣う判断である。

理由: `isExpired` / `isRevoked` / `revoke()` は同一集約 `RefreshToken` が持つ、密接に関連した 3 つの小さな振る舞い（期限切れ判定・失効判定・失効操作）であり、いずれも同じフィールド（`expiresAt` / `revoked`）に対する読み取り・書き換えの単一トランザクション的な関心事である。3 つに分割して steering を作成しても、各メソッドは 1〜2 行の自明なロジックであり検証粒度（plan-verifier によるレビューの精度）が向上するとは考えにくい一方、`AuthService#refresh` 側の 1 箇所の判定式（`oldToken.revoked() || oldToken.expiresAt().isBefore(now)`）を単一の増分でまとめて置き換え可能な形に整えるという設計上の一貫性を保つため、1 増分として扱う。

### 2. 「内側レイヤーへの契約」「ドメインモデルの利用」の「該当なし」判定

判定早見表は作業対象の種別が DomainObject の場合、「内側スタブを作成・登録」「DomainObject/VO を作成」のいずれも ✓（該当あり）と機械的に判定される。本 steering ではこれをそれぞれ「該当なし」としており、判定早見表との表面上の矛盾がある。各メソッド単位での逸脱理由は以下のとおり。

- **`isExpired(Instant now)`**: 自身が保持する `expiresAt`（`Instant` 型フィールド）と引数 `now` を比較するのみ。Repository・ドメインポート等の内側レイヤーを呼び出さず、新たな VO も生成しない。
- **`isRevoked()`**: 自身が保持する `revoked`（`boolean` 型フィールド）を返すのみ。内側レイヤー呼び出し・新規 VO 生成のいずれも発生しない。
- **`revoke()`**: 自身が保持する既存フィールド（`RefreshTokenId id` / `UserId userId` / `TokenHash tokenHash` / `Instant expiresAt`）と `revoked = true` から新しい `RefreshToken` インスタンスを `new` するのみ。この `new RefreshToken(...)` は「新規に VO/DomainObject を作成する」には当たらない（既存の `RefreshToken` 型自身の新しいインスタンスを生成するだけであり、新しい型を定義しない）。Repository・ドメインポート等の内側レイヤーへの依存も発生しない。

以上より、3 メソッドいずれも「内側はまだ存在しないためスタブを新規作成する」という判定早見表の前提（内側レイヤーへ新たに依存する）に該当しないため、意図的に「該当なし」としている。無言の逸脱ではなく、本注記により明示する。

## 作業対象レイヤー
domain（DomainObject）

## 作業対象の種別
DomainObject

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 (Jupiter) + AssertJ（`assertThat` / `assertThatThrownBy` / `assertThatCode`）。Mockito は不要（プレーンな JUnit、Spring 未起動）。
- Spring MVC アノテーション: 使用しない。

## 隣接レイヤー
- 1 つ外側のレイヤー: application（`AuthService#refresh`）
- 1 つ内側のレイヤー: なし（`isExpired` / `isRevoked` / `revoke()` はいずれも自身のフィールドのみを参照する純粋なロジックであり、内側レイヤー（Repository・ドメインポート等）を新たに呼び出さない。理由は「注意」欄を参照）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる。

- `application/AuthService.java`（`refresh` メソッド、110行目）
  - メソッド名: `refresh`
  - 引数: `@NonNull RefreshCommand command`
  - 戻り値: `RefreshResult`
  - 補足: 現状は `RefreshToken` に対して `oldToken.revoked()` / `oldToken.expiresAt()`（record アクセサ）を直接参照し、`new RefreshToken(...)` で失効済みインスタンスを直接生成している（116〜132行目）。本増分は `AuthService` 自体を変更しないが、後続の別 steering でこれらの箇所が `oldToken.isRevoked()` / `oldToken.isExpired(now)` / `oldToken.revoke()` へ置き換えられる前提でメソッドを設計する。

## 作業対象メソッドのシグネチャ
作業概要を達成するための 3 つのメソッド（`RefreshToken` の instance method）。単一メソッド原則からの逸脱理由は「注意」欄を参照。

- メソッド名: `isExpired`
  - 引数: `Instant now`
  - 戻り値: `boolean`（`expiresAt.isBefore(now)` の結果）
- メソッド名: `isRevoked`
  - 引数: なし
  - 戻り値: `boolean`（`revoked` フィールドの値）
- メソッド名: `revoke`
  - 引数: なし
  - 戻り値: `RefreshToken`（`revoked = true` とした新しいインスタンス。`id` / `userId` / `tokenHash` / `expiresAt` は元の値を引き継ぐ）

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
該当なし。判定早見表上は DomainObject 種別のため ✓ と機械的に判定されるが、`isExpired` / `isRevoked` / `revoke()` はいずれも自身が保持する既存フィールド（`RefreshTokenId` / `UserId` / `TokenHash` / `Instant` / `boolean`）のみを用いた純粋なロジックであり、新たな Repository・ドメインポート・スタブを必要としない。メソッド単位の詳細な逸脱理由は「注意」欄を参照。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
プリミティブ型への依存を禁止する。`isExpired` の引数 `Instant now` は日時のプリミティブ相当だが、`AuthService#refresh` が既に `clock.instant()`（`java.time.Instant`）を扱っており、本プロジェクトでは `Instant` を DomainObject の日時表現としてそのまま使用する既存方針（`RefreshToken#expiresAt` も `Instant` 型）に倣う。判定早見表上は DomainObject 種別のため「DomainObject/VO を作成」が ✓ と機械的に判定されるが、本増分で新規に作成する DomainObject / ValueObject はない（既存の `RefreshTokenId` / `UserId` / `TokenHash` をフィールドとしてそのまま利用し、`revoke()` が生成するのも既存型 `RefreshToken` 自身の新しいインスタンスにすぎない）。メソッド単位の詳細な逸脱理由は「注意」欄を参照。

- 作成する DomainObject / ValueObject 名: なし（新規作成なし。既存の `RefreshToken`（本体）に instance method を追加するのみ）

## テスト仕様の詳細（補足）
`RefreshTokenTest.java`（`domain/model/RefreshTokenTest.java`）を新規作成する。命名規則・`@DisplayName` は `docs/testing-guidelines.md` に従う（`正常系:` / `異常系:` 接頭辞、`methodName_condition()`）。`UserTest` と同様、テストメソッド内でファクトリヘルパー（例: `newToken(Instant expiresAt, boolean revoked)`）を用いてテストデータを直接生成する。

- 正常系: `isExpired(now)` — `expiresAt` が `now` より未来の場合は `false` を返すこと。
- 異常系（境界値/期限切れ）: `isExpired(now)` — `expiresAt` が `now` より過去の場合は `true` を返すこと。
- 正常系: `isRevoked()` — `revoked=false` で生成したインスタンスは `false` を返すこと。
- 異常系（失効済み）: `isRevoked()` — `revoked=true` で生成したインスタンスは `true` を返すこと。
- 正常系: `revoke()` — `revoked=false` のインスタンスに対して呼び出すと、`revoked=true` の新しいインスタンスを返し、かつ `id` / `userId` / `tokenHash` / `expiresAt` は元のインスタンスと同じ値を保持すること（元のインスタンス自体は変更されない＝immutable であること）。
