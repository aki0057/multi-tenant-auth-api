# requirements

## 作業概要

refresh エンドポイント残作業「増分B：infrastructure / config 改修（永続化・セキュリティ実装＋設定）」（`tmp.md` の「増分B」セクション、項目1〜12、2026-07-04 決定反映済み）を実施する。増分A（`domain/model/RefreshToken` への `tenantId` 追加、`domain/service/RefreshTokenExpirationPolicy` ポート新設、`AuthService` のポート化。コミット `5811e1b`）は完了済みであり、本増分はその内側（infrastructure 層）の未実装スタブを実装へ置換し、付随する設定変更を行う。

対象は次の12項目（`tmp.md` の番号に対応）。

1. JPA Auditing 導入（新規）: `@EnableJpaAuditing` の設定クラスのみを `config/JpaAuditingConfig.java` として新規作成する。**AuditorAware Bean は作らない**。自動管理は日時カラム（`@CreatedDate` / `@LastModifiedDate`）のみ。適用対象は本増分で新規作成する `RefreshTokenJpaEntity` のみ（既存の `UserJpaEntity` / `TenantJpaEntity` は変更しない）。
2. `infrastructure/persistence/entity/RefreshTokenJpaEntity` を新規作成する。`tenant` / `user` を `@ManyToOne(fetch = LAZY)` で既存スタイル（`UserJpaEntity`）に揃える。`token_hash` は unique、`expires_at` は `OffsetDateTime`（ドメインの `Instant` とはマッパーで変換）。日時カラムは `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate` / `@LastModifiedDate` で自動管理し、`created_by` / `updated_by` は Auditing 対象外として `RefreshTokenMapper` が明示設定する。`created_at` / `created_by` は `@Column(updatable = false)` とし、`revoke` の UPDATE 経路で NOT NULL 違反・上書きが起きないようにする。
3. `infrastructure/persistence/repository/RefreshTokenJpaRepository` を新規作成する（Spring Data JPA、`token_hash` による検索を提供）。
4. `infrastructure/persistence/mapper/RefreshTokenMapper` を新規作成する（MapStruct、`componentModel = "spring"`）。`RefreshTokenJpaEntity` ↔ `RefreshToken` の相互変換。日時カラムは変換対象外。`created_by` / `updated_by` はドメインの `userId` を文字列化して設定する。ドメイン→エンティティの `@ManyToOne` 参照（`tenant` / `user`）の解決方法は本 steering で確定する（「設計判断: @ManyToOne 参照の解決方法」参照）。
5. `infrastructure/persistence/repository/RefreshTokenRepositoryImpl` を新規作成し、`StubRefreshTokenRepository` を削除する。`findByTokenHash` / `save` を実装する。`save` は `id` が `null` → INSERT、`id` あり（revoke 済み）→ UPDATE の両経路を扱う（「設計判断: save の INSERT / UPDATE 両対応」参照）。
6. `UserRepositoryImpl#findById` を実装する（スタブ解消）。`UserJpaRepository#findById` + `UserMapper#toDomain` で実装する。
7. `infrastructure/security/SecureRandomRefreshTokenGenerator` を新規作成し、`StubRefreshTokenGenerator` を削除する。`SecureRandom` で 256bit のエントロピーを持つ生トークンを URL-safe Base64 文字列として生成する。
8. `infrastructure/security/Sha256RefreshTokenHasher` を新規作成し、`StubRefreshTokenHasher` を削除する。SHA-256 ハッシュを `TokenHash` の期待形式（64桁 hex 文字列）で返す。
9. `infrastructure/security/JwtProperties` に `refreshExpiration`（`jwt.refresh-expiration` にバインド）フィールドを追加する。`RefreshTokenExpirationPolicy` の本実装 `infrastructure/security/JwtRefreshTokenExpirationPolicy` を新規作成し（`JwtProperties.refreshExpiration()` を返す）、`StubRefreshTokenExpirationPolicy` を削除する。
10. 設定ファイル修正: `application-test.properties` に `jwt.refresh-expiration=14d` を追加する（14d で確定済み）。`application.properties` への `jwt.refresh-expiration=${JWT_REFRESH_EXPIRATION}` および `.env.example` への `JWT_REFRESH_EXPIRATION="14d"` は増分Aのコミット `5811e1b` で追加済みのため対象外。`.env` へのユーザーのローカル追加も対象外。
11. `TODO.md` の該当項目（`Repository (domain)` / `Port (domain)` / `infrastructure.security` / `infrastructure.persistence` の未チェック項目）を実装完了に更新する。新規作成する `RefreshTokenMapper` の各メソッドは `infrastructure.mapper` 章に新規追記（`UserMapper#toDomain(UserJpaEntity)` と同じ扱いで、作成と同時に完成として `[x]`）する。
12. `config/SecurityConfig` の暫定 TODO コメント（`// TODO: refreshエンドポイントの認可状態は暫定である`）を削除する（`/refresh` の `permitAll` 自体は変更しない）。

## 注意（テンプレートからの逸脱と理由）

本 steering は次の 2 点でテンプレート・判定早見表の機械的な適用から逸脱する。分割せず 1 増分として扱うことが妥当かどうかの最終判断はゲートA（ユーザー承認）に委ねる。先例 `.steering/20260628-1031-refactor-user-authenticate`・`.steering/20260704-1336-feat-refreshtoken-tenantid-expiration-policy` に倣う。

### 1. 単一種別・単一メソッド原則からの逸脱

本増分は `tmp.md` で明示的に「増分B」として一括指示された、密接に関連する複数のクラス作成・修正（永続化・セキュリティ実装・設定）から構成される。これらはいずれも増分Aで新設された domain ポート／リポジトリインターフェースの内側実装であり、相互依存が強い（例: `RefreshTokenRepositoryImpl` は `RefreshTokenMapper` に依存し、`RefreshTokenMapper` は `RefreshTokenJpaEntity` に依存する）ため、分割すると個々の steering 単独ではコンパイルが通らない、または `@SpringBootTest` のコンテキスト起動が通らない（スタブが残ったままの実装が混在する）。したがって `tmp.md` の増分単位のまま 1 steering として扱う。

### 2. 判定早見表の機械的な「該当あり／なし」に対する明示

判定早見表は `infrastructure.mapper` / `infrastructure.security` の種別について、「内側スタブを作成・登録」「DomainObject/VO を作成」のいずれも ✓ と機械的に判定される。本増分ではこれらを次のとおり扱う（無言の逸脱ではなく明示する）。

- **内側スタブの作成**: infrastructure 層は本プロジェクトのアーキテクチャで最も内側のレイヤーであり、これより内側の呼び出しは発生しない。したがって本増分ではいかなる新規スタブも作成しない（「内側レイヤーへの契約」は「該当なし」とする）。
- **DomainObject / VO の新規作成**: 本増分は増分Aで確定済みの既存 VO（`TenantId` / `UserId` / `TokenHash` / `RawRefreshToken` / `RefreshTokenId`）と既存 DomainObject（`RefreshToken` / `User`）をそのまま利用する。新規の DomainObject / VO 作成はない。

### 3. tester red に伴う `JpaAuditingConfig` への DateTimeProvider Bean 追加（差し戻し対応）

当初は「`@EnableJpaAuditing` のみ」の設定クラスとしていたが、tester 段階で `RefreshTokenJpaRepositoryTest`（2件）・`RefreshTokenRepositoryImplTest`（4件）が red となった。原因は、Spring Data Auditing のデフォルト `DateTimeProvider`（`CurrentDateTimeProvider`）が `LocalDateTime` を返し、`RefreshTokenJpaEntity` の `createdAt` / `updatedAt`（`OffsetDateTime`）への変換をサポートしていないこと（`Cannot convert unsupported date type java.time.LocalDateTime to java.time.OffsetDateTime`）。

対応として、エンティティの型（既存の `UserJpaEntity` / `TenantJpaEntity` と揃えた `OffsetDateTime`）は維持し、`JpaAuditingConfig` に `OffsetDateTime` を供給する `DateTimeProvider` Bean（Bean 名: `auditingDateTimeProvider`）を追加して `@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")` で参照する。本欄で禁止しているのは AuditorAware Bean（監査「者」の解決）であり、DateTimeProvider（監査「日時」の供給）はこれに抵触しない。AuditorAware Bean は引き続き作らない。

## 作業対象レイヤー

infrastructure（主軸: `infrastructure/persistence` の Repository 実装）。付随して infrastructure/persistence（entity・Spring Data JPA リポジトリ・MapStruct マッパー）・infrastructure/security（ドメインポート実装）・config（`@Configuration` クラス・既存 `SecurityConfig` のコメント削除）を含む、`tmp.md` の増分Bで指示された複数種別の変更。

## 作業対象の種別

Repository (domain) の実装（主軸）＋ infrastructure.mapper ＋ infrastructure.security ＋ 設定ファイル・`config/`。

いずれも「外側（domain の Repository インターフェース／ドメインポート）は増分Aまでに確定済みで、内側（infrastructure 実装）が未実装」という同一の構造（判定早見表の Repository 行・infrastructure.mapper 行・infrastructure.security 行）を持つため、1 steering にまとめて扱う。

## 使用するテスト・フレームワーク等

`docs/testing-guidelines.md`「レイヤー別テスト方針」に従い、種別ごとに以下のテスト方式を用いる。

| 対象 | テスト方式 |
|---|---|
| `RefreshTokenJpaRepository`（Spring Data JPA） | `@DataJpaTest` + `TestEntityManager`（実 H2 DB、モックなし） |
| `RefreshTokenRepositoryImpl`（リポジトリ実装） | `@DataJpaTest` + `TestEntityManager`。`RefreshTokenJpaRepository` / `TenantJpaRepository` / `UserJpaRepository` は `@Autowired` で取得し、`RefreshTokenMapperImpl` と `RefreshTokenRepositoryImpl` はテスト内で `new` して手動配線する（`UserRepositoryImpl` 同様、専用の Spring Bean 化されたリポジトリ実装クラスの単体テストは本プロジェクトでは作成しない前例があるため、`RefreshTokenRepositoryImpl` は本増分で新規に直接テストする）。 |
| `UserRepositoryImpl#findById` | 既存の `UserJpaRepositoryTest`（`@DataJpaTest`）と `UserMapperTest` で間接的にカバーされる基盤に対し、`findById` 固有の振る舞い（存在する/しない）を確認する必要があるため、`UserJpaRepositoryTest` に `findById` のテストケースを追加する（新規テストクラスは作らない）。 |
| `RefreshTokenMapper`（MapStruct マッパー） | Spring 未起動。`new RefreshTokenMapperImpl()` で直接インスタンス化（`UserMapperTest` に倣う）。 |
| `SecureRandomRefreshTokenGenerator` / `Sha256RefreshTokenHasher` / `JwtRefreshTokenExpirationPolicy`（技術アダプター） | Spring 未起動。コンストラクタで直接インスタンス化（`PasswordEncoderVerifierTest` に倣う）。 |
| `JpaAuditingConfig` / `SecurityConfig` のコメント削除 | 専用テストは作成しない（`docs/testing-guidelines.md` の「テストを作成しない種別」に該当。結合テストで間接的に検証済み）。 |

- テストフレームワーク: JUnit 5 (Jupiter) + AssertJ。Spring Data JPA 関連は `@DataJpaTest`。Mockito は使用しない（本増分の対象クラスはいずれもモック対象の外部依存を持たない、または実 DB を使う方針のため）。
- Spring MVC アノテーション: 使用しない。

## 隣接レイヤー

- 1 つ外側のレイヤー: domain（`domain/repository/RefreshTokenRepository` / `domain/repository/UserRepository` / `domain/service/RefreshTokenGenerator` / `domain/service/RefreshTokenHasher` / `domain/service/RefreshTokenExpirationPolicy`。いずれも増分Aまでに確定済み）。
- 1 つ内側のレイヤー: 該当なし（infrastructure は最内層であり、これより内側は存在しない）。

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)

外側（domain 層のインターフェース）はいずれも既に存在し、本増分ではシグネチャを変更しない。以下は本増分が実装する契約点である。

- `domain/repository/RefreshTokenRepository`
  - `Optional<RefreshToken> findByTokenHash(TokenHash tokenHash)`
  - `RefreshToken save(RefreshToken refreshToken)`
- `domain/repository/UserRepository`
  - `Optional<User> findById(UserId userId)`
- `domain/service/RefreshTokenGenerator`
  - `RawRefreshToken generate()`
- `domain/service/RefreshTokenHasher`
  - `TokenHash hash(RawRefreshToken rawRefreshToken)`
- `domain/service/RefreshTokenExpirationPolicy`
  - `Duration expiration()`

## 作業対象メソッドのシグネチャ

作業概要を達成するための複数の変更対象（単一メソッド原則からの逸脱理由は「注意」欄を参照）。詳細は「変更内容の詳細」の一覧表を参照。

- `RefreshTokenRepositoryImpl`
  - `findByTokenHash(TokenHash tokenHash): Optional<RefreshToken>`
  - `save(RefreshToken refreshToken): RefreshToken`
- `UserRepositoryImpl`
  - `findById(UserId userId): Optional<User>`（既存メソッドのスタブ解消。シグネチャは `UserRepository` に確定済みのため不変）
- `SecureRandomRefreshTokenGenerator`
  - `generate(): RawRefreshToken`
- `Sha256RefreshTokenHasher`
  - `hash(RawRefreshToken rawRefreshToken): TokenHash`
- `JwtRefreshTokenExpirationPolicy`
  - `expiration(): Duration`
- `RefreshTokenMapper`（新設、MapStruct）
  - `toDomain(RefreshTokenJpaEntity entity): RefreshToken`
  - `toEntity(RefreshToken domain, TenantJpaEntity tenant, UserJpaEntity user): RefreshTokenJpaEntity`

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)

該当なし。infrastructure は本プロジェクトのアーキテクチャで最も内側のレイヤーであり、これより内側の呼び出し・新規スタブ作成は発生しない（「注意」欄の逸脱点 2 を参照）。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)

プリミティブ型への依存を禁止する。本増分で新規に作成する DomainObject / ValueObject はない。すべて増分Aまでに確定済みの既存 VO / DomainObject をそのまま利用する。

- `TenantId`（既存、`domain/model/vo/TenantId.java`） — `RefreshTokenMapper` がエンティティ⇔ドメイン変換に使用
- `UserId`（既存、`domain/model/vo/UserId.java`） — 同上、および `UserRepositoryImpl#findById` の引数
- `TokenHash`（既存、`domain/model/vo/TokenHash.java`） — `RefreshTokenRepositoryImpl` / `Sha256RefreshTokenHasher` が使用
- `RawRefreshToken`（既存、`domain/model/vo/RawRefreshToken.java`） — `SecureRandomRefreshTokenGenerator` / `Sha256RefreshTokenHasher` が使用
- `RefreshTokenId`（既存、`domain/model/vo/RefreshTokenId.java`） — `RefreshTokenMapper` がエンティティ⇔ドメイン変換に使用
- `RefreshToken`（既存 DomainObject、`domain/model/RefreshToken.java`） — `RefreshTokenRepositoryImpl` / `RefreshTokenMapper` が入出力に使用
- `User`（既存 DomainObject、`domain/model/User.java`） — `UserRepositoryImpl#findById` の戻り値
- 作成する DomainObject / ValueObject 名: なし（新規作成なし）

## 設計判断: @ManyToOne 参照の解決方法

`RefreshTokenMapper#toEntity` はドメイン→エンティティ変換時に `tenant` / `user`（`@ManyToOne`）を必要とするが、ドメインの `RefreshToken` は `TenantId` / `UserId`（主キー値）しか持たない。以下の方針で解決する。

1. `RefreshTokenMapper#toEntity` は **参照解決を行わない**。呼び出し側（`RefreshTokenRepositoryImpl`）が解決済みの `TenantJpaEntity` / `UserJpaEntity` を引数として渡すシグネチャ（`toEntity(RefreshToken domain, TenantJpaEntity tenant, UserJpaEntity user)`）とする。マッパーの責務をエンティティ⇔ドメインの「フィールド変換」に限定し、DB アクセスを持ち込まない（`UserMapper` が Spring Data / EntityManager に依存しない既存方針と整合させる）。
2. 参照解決は `RefreshTokenRepositoryImpl#save` 内で、主キーのみ確定していれば十分な `JpaRepository#getReferenceById`（プロキシを返し、追加の SELECT を発行しない）を用いて行う。
   - `user` の解決には既存の `UserJpaRepository`（`JpaRepository<UserJpaEntity, Long>` を継承済み）の `getReferenceById(Long)` をそのまま利用する。
   - `tenant` の解決には対応する Spring Data JPA インターフェースが存在しないため、本増分で `infrastructure/persistence/repository/TenantJpaRepository`（`JpaRepository<TenantJpaEntity, Long>` を継承するだけの最小インターフェース。カスタムクエリメソッドは追加しない）を新規作成する。`tmp.md` の「`getReferenceById` を `RefreshTokenRepositoryImpl` 側で行う等」の例示に沿う選択肢の一つであり、`EntityManager` を直接注入する方式より、既存の `UserJpaRepository` と対称的で `@RequiredArgsConstructor` によるコンストラクタ注入と相性が良く、テスト時も `@DataJpaTest` で素直に `@Autowired` できる（`EntityManager` はコンストラクタ注入に不向き。`@PersistenceContext` はフィールド/セッターインジェクションが基本のため）という利点を理由に採用する。
   - `TenantJpaRepository` はカスタムクエリメソッドを持たない（`getReferenceById` は継承元 `JpaRepository` のメソッドで足りる）ため、`TODO.md` への新規登録は行わない（既存の `UserJpaRepository` も TODO.md 未登録であることと整合）。専用の単体テストも作成しない（`RefreshTokenRepositoryImplTest` 経由で間接的に検証する）。

## 設計判断: RefreshTokenRepositoryImpl#save の INSERT / UPDATE 両対応

`AuthService#refresh` は同一の `save(RefreshToken)` を、旧トークンの失効更新（`id` あり）と新トークンの新規作成（`id` が `null`）の両方で呼び出す。`RefreshTokenRepositoryImpl#save` はどちらの経路でも分岐せず同一の実装とし、次の手順で行う。

1. `refreshToken.tenantId()` / `refreshToken.userId()` から `tenantJpaRepository.getReferenceById(...)` / `userJpaRepository.getReferenceById(...)` でプロキシを取得する。
2. `refreshTokenMapper.toEntity(refreshToken, tenant, user)` でエンティティを構築する（`id` は `refreshToken.id()` が `null` ならエンティティも `null`。`createdBy` / `updatedBy` はいずれも `String.valueOf(refreshToken.userId().value())` を設定する）。
3. `refreshTokenJpaRepository.save(entity)` を呼ぶ。Spring Data JPA は `id` が `null` なら `persist`（INSERT）、`id` が非 `null` なら `merge`（UPDATE）を自動選択する。
4. UPDATE 経路（`merge`）で `createdAt` / `createdBy` に手元のエンティティの値（本来 DB の既存値ではなく、マッパーが today 生成した値）が使われて NOT NULL 違反・上書きが起きないよう、`RefreshTokenJpaEntity` の `created_at` / `created_by` カラムは `@Column(updatable = false)` を付与済みである（tmp.md 項目1・2 の決定）。`updatable = false` により、これらのカラムは生成される UPDATE SQL の SET 句に含まれないため、エンティティ側の値が `null`（またはマッパーが設定した任意の値）であっても DB の既存の値は変更されず、NOT NULL 違反も発生しない。
5. `updatedAt` は `@LastModifiedDate` により Auditing が UPDATE 時に自動更新する。`updatedBy` はマッパーが明示設定した値がそのまま UPDATE される（Auditing 対象外のため）。

この設計により、`save` は分岐なしの単純な実装で INSERT / UPDATE の両方を安全に扱える。

## 変更内容の詳細（補足）

| 操作 | ファイルパス（`src/` 以下） | 内容 |
|---|---|---|
| 新規作成 | `main/java/.../config/JpaAuditingConfig.java` | `@Configuration` + `@EnableJpaAuditing`。AuditorAware Bean は定義しない |
| 新規作成 | `main/java/.../infrastructure/persistence/entity/RefreshTokenJpaEntity.java` | `@Entity @Table(name = "refresh_tokens")`。`tenant` / `user` は `@ManyToOne(fetch = LAZY, optional = false)`。`tokenHash`（unique）、`expiresAt`（`OffsetDateTime`）、`revoked`（`is_revoked`）。`createdAt` / `updatedAt` は `@CreatedDate` / `@LastModifiedDate` + `@EntityListeners(AuditingEntityListener.class)`。`createdAt` / `createdBy` は `@Column(updatable = false)` |
| 新規作成 | `main/java/.../infrastructure/persistence/repository/RefreshTokenJpaRepository.java` | `JpaRepository<RefreshTokenJpaEntity, Long>`。`Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash)` |
| 新規作成 | `main/java/.../infrastructure/persistence/repository/TenantJpaRepository.java` | `JpaRepository<TenantJpaEntity, Long>`（カスタムメソッドなし。`getReferenceById` 利用のためのみ新規作成） |
| 新規作成 | `main/java/.../infrastructure/persistence/mapper/RefreshTokenMapper.java` | MapStruct、`componentModel = "spring"`。`toDomain(RefreshTokenJpaEntity): RefreshToken` / `toEntity(RefreshToken, TenantJpaEntity, UserJpaEntity): RefreshTokenJpaEntity`。`createdBy` / `updatedBy` はドメインの `userId` を文字列化して設定。`createdAt` / `updatedAt` はマッピング対象外 |
| 新規作成 | `main/java/.../infrastructure/persistence/repository/RefreshTokenRepositoryImpl.java` | `RefreshTokenRepository` 実装。`findByTokenHash` / `save` を実装（詳細は「設計判断」参照） |
| 削除 | `main/java/.../infrastructure/persistence/repository/StubRefreshTokenRepository.java` | `RefreshTokenRepositoryImpl` へ置換のため削除 |
| 変更 | `main/java/.../infrastructure/persistence/repository/UserRepositoryImpl.java` | `findById` のスタブ実装を `userJpaRepository.findById(userId.value()).map(userMapper::toDomain)` へ置換。`// TODO` コメント削除 |
| 新規作成 | `main/java/.../infrastructure/security/SecureRandomRefreshTokenGenerator.java` | `RefreshTokenGenerator` 実装。`SecureRandom` で 256bit のランダムバイト列を生成し URL-safe Base64（パディングなし）で文字列化 |
| 削除 | `main/java/.../infrastructure/security/StubRefreshTokenGenerator.java` | 上記へ置換のため削除 |
| 新規作成 | `main/java/.../infrastructure/security/Sha256RefreshTokenHasher.java` | `RefreshTokenHasher` 実装。`MessageDigest.getInstance("SHA-256")` でハッシュ化し 64 桁 hex 文字列化 |
| 削除 | `main/java/.../infrastructure/security/StubRefreshTokenHasher.java` | 上記へ置換のため削除 |
| 変更 | `main/java/.../infrastructure/security/JwtProperties.java` | `refreshExpiration`（`Duration`）フィールドを追加。`jwt.refresh-expiration` にバインド |
| 新規作成 | `main/java/.../infrastructure/security/JwtRefreshTokenExpirationPolicy.java` | `RefreshTokenExpirationPolicy` 実装。`@Component` + `@RequiredArgsConstructor`。`JwtProperties.refreshExpiration()` をそのまま返す |
| 削除 | `main/java/.../infrastructure/security/StubRefreshTokenExpirationPolicy.java` | 上記へ置換のため削除 |
| 変更 | `main/java/.../config/SecurityConfig.java` | `// TODO: refreshエンドポイントの認可状態は暫定である` コメント行を削除（`permitAll` 設定自体は変更しない） |
| 変更 | `src/test/resources/application-test.properties` | `jwt.refresh-expiration=14d` を追加 |
| 新規作成 | `test/java/.../infrastructure/persistence/repository/RefreshTokenJpaRepositoryTest.java` | `@DataJpaTest` + `TestEntityManager`。`findByTokenHash` の正常系・異常系 |
| 新規作成 | `test/java/.../infrastructure/persistence/repository/RefreshTokenRepositoryImplTest.java` | `@DataJpaTest` + `TestEntityManager`。`findByTokenHash` / `save`（INSERT・UPDATE 両経路）の正常系・異常系 |
| 変更 | `test/java/.../infrastructure/persistence/repository/UserJpaRepositoryTest.java` | `findById` の正常系（存在する）・異常系（存在しない）テストケースを追加 |
| 新規作成 | `test/java/.../infrastructure/persistence/mapper/RefreshTokenMapperTest.java` | `new RefreshTokenMapperImpl()` を直接インスタンス化。`toDomain` / `toEntity` の正常系・異常系 |
| 新規作成 | `test/java/.../infrastructure/security/SecureRandomRefreshTokenGeneratorTest.java` | 正常系（生成値が空でない・毎回異なる）・異常系は該当なしのため省略可（詳細は tasklist 参照） |
| 新規作成 | `test/java/.../infrastructure/security/Sha256RefreshTokenHasherTest.java` | 正常系（既知の入力に対する SHA-256 ハッシュ値の一致・64桁 hex）・異常系は該当なしのため省略可 |
| 新規作成 | `test/java/.../infrastructure/security/JwtRefreshTokenExpirationPolicyTest.java` | 正常系（`JwtProperties.refreshExpiration()` の値がそのまま返る） |
| 変更 | `TODO.md` | 「TODO.md との対応」参照 |

## TODO.md との対応

以下の未チェック項目を実装完了として `[x]` に更新する。

- `Repository (domain)` 章: `[ ] findById（UserRepository）` / `[ ] RefreshTokenRepository#findByTokenHash` / `[ ] RefreshTokenRepository#save`
- `Port (domain)` 章: `[ ] RefreshTokenGenerator#generate` / `[ ] RefreshTokenHasher#hash` / `[ ] RefreshTokenExpirationPolicy#expiration`
- `infrastructure.security` 章: `[ ] StubRefreshTokenGenerator を実装へ置換` / `[ ] StubRefreshTokenHasher を実装へ置換` / `[ ] StubRefreshTokenExpirationPolicy を実装へ置換`
- `infrastructure.persistence` 章: `[ ] StubRefreshTokenRepository を実装へ置換` / `[ ] UserRepositoryImpl#findById を実装`

以下は新規追記し、作成と同時に完成扱いとして `[x]` にする（`infrastructure.mapper` 章、`UserMapper#toDomain(UserJpaEntity)` と同じ扱い）。

- `infrastructure.mapper` 章: `[x] RefreshTokenMapper#toDomain(RefreshTokenJpaEntity)` / `[x] RefreshTokenMapper#toEntity(RefreshToken, TenantJpaEntity, UserJpaEntity)`

`RefreshTokenJpaEntity` / `RefreshTokenJpaRepository` / `TenantJpaRepository`（JPA エンティティ・Spring Data JPA インターフェース）は、既存の `UserJpaEntity` / `UserJpaRepository` が TODO.md 未登録であることと整合させ、新規登録しない。
