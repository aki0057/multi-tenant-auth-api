# requirements

## 作業概要

refresh エンドポイント残作業「増分A：domain / application 改修（tenant_id 対応＋有効期間のポート化）」（`tmp.md` 参照）を実施する。次の変更を行う。

1. `domain/model/RefreshToken`（record）に `TenantId tenantId` を追加する。フィールド順は `(id, tenantId, userId, tokenHash, expiresAt, revoked)`。`revoke()` は tenantId を含む全フィールドを引き継ぐ。
2. 新ドメインポート `domain/service/RefreshTokenExpirationPolicy` を新規作成する。リフレッシュトークンの有効期間（`java.time.Duration`）を返す。実装は本増分では作らず、`@SpringBootTest` のコンテキスト起動を通すためのスタブのみ infrastructure 層に用意する（本実装は後続の別増分＝増分Bで `JwtProperties` を参照して行う）。
3. `application/AuthService` を修正する。`REFRESH_TOKEN_EXPIRATION = Duration.ofDays(14)` 定数を削除し、新ポート `RefreshTokenExpirationPolicy` から取得した `Duration` で `expiresAt` を計算する（`Clock` 注入方式は維持）。新しい `RefreshToken` 生成時に `user.tenantId()` を渡す。
4. 新ポートのスタブ実装 `StubRefreshTokenExpirationPolicy` を `infrastructure/security/` に新規作成する（`@Component`、中身は `UnsupportedOperationException`。既存の `StubRefreshTokenGenerator` / `StubRefreshTokenHasher` と同じ規約）。
5. `RefreshTokenTest` / `AuthServiceTest` を新シグネチャに追随させる。
6. `TODO.md` にスタブ（新ポート `RefreshTokenExpirationPolicy#expiration` ＋スタブ実装 `StubRefreshTokenExpirationPolicy`）を追記する。

**ユーザー確定済みの追加決定（2026-07-04）**: refresh 処理で旧トークンの `tenantId` とユーザーの `tenantId` の一致チェックは追加しない。新トークン生成時に `user.tenantId()` を渡すのみ。`application.properties` の修正は本増分の対象外（増分Bで実施）。

## 注意（テンプレートからの逸脱と理由）

本 steering はテンプレート・判定早見表の機械的な適用に対して以下 2 点で逸脱する。分割せず 1 増分として扱うことが妥当かどうかの最終判断はゲートA（ユーザー承認）に委ねる。先例 `.steering/20260628-1031-refactor-user-authenticate`（`User#authenticate` を中心に domain/service ポート新設・application 変更・infrastructure アダプタ新設へ波及したリファクタリングを 1 増分にまとめた事例）に倣う。

### 1. 「作業対象メソッドのシグネチャ」の単一メソッド原則からの逸脱

本 steering は次の複数の変更をまとめて扱う。

- `RefreshToken` record への `tenantId` フィールド追加（レコード自体のシグネチャ変更）と `revoke()` の追随
- 新設ポート `RefreshTokenExpirationPolicy#expiration()`
- `AuthService#refresh` 内部実装の変更（シグネチャ自体は不変）

理由: これらはいずれも「リフレッシュトークンの tenant_id 対応と有効期間のポート化」という単一の設計変更（tmp.md の増分A）を構成する密接に関連した変更であり、`RefreshToken` の外側（`AuthService#refresh`）が新フィールド・新ポートを同時に使い始める関係上、3 つに分割しても各 steering 単独では他方が未実装のためコンパイルが通らない（相互依存が強い）。したがって 1 増分としてまとめて扱う。

### 2. 判定早見表の機械的な「該当あり／なし」に対する明示

判定早見表は作業対象の種別が DomainObject の場合、「内側スタブを作成・登録」「DomainObject/VO を作成」のいずれも ✓ と機械的に判定される。本増分ではこれらを次のとおり扱う（無言の逸脱ではなく明示する）。

- **`RefreshToken` へのフィールド追加・`revoke()` の追随**: 新たな内側レイヤー呼び出しは発生しない。使用する `TenantId` は既存 VO（新規作成なし）。
- **新設ポート `RefreshTokenExpirationPolicy`**: 判定早見表どおり内側スタブ（`StubRefreshTokenExpirationPolicy`）を作成する。新規 VO/DomainObject の作成はない（戻り値は既存の `java.time.Duration`）。

## 作業対象レイヤー

domain（DomainObject）を主軸とした複数レイヤー変更。付随して domain/service（ドメインポート新設）・application（Service 変更）・infrastructure/security（スタブアダプタ新設）を含む。

## 作業対象の種別

DomainObject（主軸）

## 使用するテスト・フレームワーク等

- テストフレームワーク: `RefreshTokenTest` はプレーン JUnit 5 (Jupiter) + AssertJ（Spring 未起動）。`AuthServiceTest` は JUnit 5 + Mockito（`@ExtendWith(MockitoExtension.class)`、`@Mock` / `@InjectMocks`）+ AssertJ。
- Spring MVC アノテーション: 使用しない。

## 隣接レイヤー

- 1 つ外側のレイヤー: application（`AuthService#refresh`）
- 1 つ内側のレイヤー: domain/service（新設ポート `RefreshTokenExpirationPolicy`）。その実装は infrastructure/security（本増分ではスタブのみ、本実装は増分Bで `JwtProperties` を参照して行う）。

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)

外側（`AuthService#refresh`）は既に存在し、本増分でも呼び出しシグネチャ自体は変更しない。

- メソッド名: `refresh`
- 引数: `@NonNull RefreshCommand command`
- 戻り値: `RefreshResult`

`AuthService#refresh` 内部で `RefreshToken` を参照・生成する箇所（`domain/model/RefreshToken.java` の record コンストラクタ・`revoke()`）が本増分の変更対象であり、`AuthService` はその変更後シグネチャに追随する（`application/AuthService.java` 自体も本増分の変更対象。詳細は「変更内容の詳細」を参照）。

## 作業対象メソッドのシグネチャ

作業概要を達成するための複数の変更対象（単一メソッド原則からの逸脱理由は「注意」欄を参照）。

- レコード: `RefreshToken(RefreshTokenId id, TenantId tenantId, UserId userId, TokenHash tokenHash, Instant expiresAt, boolean revoked)`
  - フィールド順: `(id, tenantId, userId, tokenHash, expiresAt, revoked)`（`tenantId` を `id` の直後に追加）
- メソッド名: `revoke`
  - 引数: なし
  - 戻り値: `RefreshToken`（`revoked = true` とした新しいインスタンス。`id` / `tenantId` / `userId` / `tokenHash` / `expiresAt` は元の値を引き継ぐ）
- インターフェース: `RefreshTokenExpirationPolicy`（`domain/service/` に新規作成）
  - メソッド名: `expiration`
  - 引数: なし
  - 戻り値: `Duration`（リフレッシュトークンの有効期間）
  - 本増分では未実装のため、既存の未実装ポート（`RefreshTokenGenerator.java` / `RefreshTokenHasher.java`）と同じ規約に倣い、インターフェース宣言（`public interface RefreshTokenExpirationPolicy {`）の直上に `// TODO: 実装（後続 infrastructure 増分）` を付与する

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)

新設ポート `RefreshTokenExpirationPolicy` の内側（infrastructure 層）はまだ存在しないため、スタブとして新規作成する（実装しない・TODO のみ）。

- クラス: `StubRefreshTokenExpirationPolicy`（`infrastructure/security/` パッケージ、`@Component`、`implements RefreshTokenExpirationPolicy`）
- メソッド名: `expiration`
- 引数: なし
- 戻り値: `Duration`（常に `UnsupportedOperationException` をスローする。既存の `StubRefreshTokenGenerator` / `StubRefreshTokenHasher` と同じ規約でクラス Javadoc とメソッド直上に `// TODO: JwtProperties 参照実装へ置換（後続 infrastructure 増分）` を記載する）

## ドメインモデルの利用 (条件が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)

プリミティブ型への依存を禁止する。本増分で新規に作成する DomainObject / ValueObject はない。

- `RefreshToken` が新たに保持する `TenantId` は既存 VO（`domain/model/vo/TenantId.java`）をそのまま利用する。
- `RefreshTokenExpirationPolicy#expiration()` の戻り値 `Duration` は、既存 `AuthService` が `Clock` とともに標準ライブラリの日時型として扱っている方針（`RefreshToken#expiresAt` も `Instant`）に倣い、そのまま使用する。
- 作成する DomainObject / ValueObject 名: なし（新規作成なし）

## 変更内容の詳細（補足）

| 操作 | ファイルパス（`src/` 以下） | 内容 |
|---|---|---|
| 変更 | `main/java/.../domain/model/RefreshToken.java` | `TenantId tenantId` フィールドを `id` の次に追加し、`revoke()` を追随修正。Javadoc も更新する |
| 新規作成 | `main/java/.../domain/service/RefreshTokenExpirationPolicy.java` | ドメインポート。`Duration expiration()`。本増分では未実装のため、既存の未実装ポート（`RefreshTokenGenerator.java` / `RefreshTokenHasher.java`）と同じ規約でインターフェース宣言直上に `// TODO: 実装（後続 infrastructure 増分）` を付与する |
| 変更 | `main/java/.../application/AuthService.java` | `REFRESH_TOKEN_EXPIRATION` 定数を削除。`RefreshTokenExpirationPolicy` をフィールドとして追加（`@RequiredArgsConstructor` により自動注入）。`now.plus(REFRESH_TOKEN_EXPIRATION)` → `now.plus(refreshTokenExpirationPolicy.expiration())` へ変更。`new RefreshToken(null, user.userId(), newTokenHash, now.plus(...), false)` → `new RefreshToken(null, user.tenantId(), user.userId(), newTokenHash, now.plus(...), false)` へ変更 |
| 新規作成 | `main/java/.../infrastructure/security/StubRefreshTokenExpirationPolicy.java` | `RefreshTokenExpirationPolicy` のスタブ実装。`@Component`、`expiration()` は `UnsupportedOperationException` をスロー |
| 変更 | `test/java/.../domain/model/RefreshTokenTest.java` | `newToken(...)` ヘルパーと各テストの `new RefreshToken(...)` 呼び出しに `TenantId` を追加（フィールド順に合わせる） |
| 変更 | `test/java/.../application/AuthServiceTest.java` | `@Mock RefreshTokenExpirationPolicy refreshTokenExpirationPolicy` を追加。`refresh_success` で `when(refreshTokenExpirationPolicy.expiration()).thenReturn(Duration.ofDays(14))` をスタブし、既存の `Duration.ofDays(14)` を用いたアサーションはそのまま維持する。`validOldToken()` / `refresh_tokenRevoked` / `refresh_tokenExpired` 内の `new RefreshToken(...)` に `TenantId`（`activeUser.tenantId()` と一致する `new TenantId(1L)`）を追加する |
| 変更 | `TODO.md` | `Port (domain)` 章に `RefreshTokenExpirationPolicy#expiration` を追記、`infrastructure.security` 章に `StubRefreshTokenExpirationPolicy を実装へ置換` を追記（いずれも空チェックボックス） |

## テスト仕様の詳細（補足）

### `RefreshTokenTest`

既存の `revoke_returnsRevokedInstance` / `isValid_notRevokedAndNotExpired` / `isValid_revoked` / `isValid_expired` の 4 テストについて、`newToken(...)` ヘルパーが生成する `RefreshToken` に `TenantId`（例: `new TenantId(1L)`）を追加し、`revoke_returnsRevokedInstance` のアサーションに `assertThat(revoked.tenantId()).isEqualTo(original.tenantId())` を追加する。既存の正常系・異常系の観点自体は変更しない。

### `AuthServiceTest`

- `refresh_success`: `RefreshTokenExpirationPolicy` をモックし `Duration.ofDays(14)` を返すようスタブする。`captor` で捕捉した新規保存トークンの `tenantId()` が `activeUser.tenantId()`（`new TenantId(1L)`）と一致することをアサーションに追加する。
- `refresh_tokenRevoked` / `refresh_tokenExpired`: これらは `RefreshTokenExpirationPolicy#expiration()` を呼び出す前に例外がスローされる経路のため、モックのスタブ設定（`when(...)`）は不要（未スタブのままでよい）。`new RefreshToken(...)` の呼び出しにのみ `TenantId` 引数を追加する。
- 既存の `refresh_tokenNotFound` / `refresh_userNotFound` / `refresh_userInactive` / `refresh_tenantInactive` は `RefreshToken` の生成箇所（`validOldToken()`）を共有しているため、`validOldToken()` の修正のみで追随する。

## TODO.md との対応

以下のエントリを新規追記する（実装完了後にチェックを埋める）。

- `Port (domain)` セクション: `[ ] RefreshTokenExpirationPolicy#expiration`
- `infrastructure.security` セクション: `[ ] StubRefreshTokenExpirationPolicy を実装へ置換`

`RefreshToken`（DomainObject）は既に `TODO.md` の `DomainObject (domain)` 章に登録・完了済み（`[x] RefreshToken`）のため、本増分によるフィールド追加では新規登録・再登録は不要。
