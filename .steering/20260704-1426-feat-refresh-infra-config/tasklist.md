# tasklist

## 1. JPA Auditing 導入

- [x] `config/JpaAuditingConfig.java` を新規作成する（`@Configuration` + `@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")`。`OffsetDateTime` を供給する `DateTimeProvider` Bean（Bean 名: `auditingDateTimeProvider`）を定義する。AuditorAware Bean は定義しない。DateTimeProvider Bean 追加の経緯は requirements.md「注意」欄の逸脱点 3 を参照）
- [x] Javadoc を記載する

## 2. RefreshTokenJpaEntity の新規作成

- [x] `infrastructure/persistence/entity/RefreshTokenJpaEntity.java` を新規作成する（`@Entity @Table(name = "refresh_tokens")`。`id`（`@GeneratedValue(IDENTITY)`）、`tenant` / `user`（`@ManyToOne(fetch = LAZY, optional = false)` + `@JoinColumn`）、`tokenHash`（`token_hash`, unique, not null）、`expiresAt`（`expires_at`, `OffsetDateTime`, not null）、`revoked`（`is_revoked`, not null）、`createdAt`（`@CreatedDate`, `updatable = false`）、`updatedAt`（`@LastModifiedDate`）、`createdBy`（`updatable = false`）、`updatedBy`。`@EntityListeners(AuditingEntityListener.class)` を付与。既存の `UserJpaEntity` / `TenantJpaEntity` と同じ Lombok 構成（`@Getter` `@NoArgsConstructor(PROTECTED)` `@AllArgsConstructor(PRIVATE)` `@Builder`）に揃える）
- [x] Javadoc を記載する

## 3. RefreshTokenJpaRepository の新規作成

- [x] `infrastructure/persistence/repository/RefreshTokenJpaRepository.java` を新規作成する（`JpaRepository<RefreshTokenJpaEntity, Long>`。`Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash)`）
- [x] Javadoc を記載する
- [x] `RefreshTokenJpaRepositoryTest.java` を新規作成し、`@DataJpaTest` + `TestEntityManager` で正常系のテストコードを記載する（`findByTokenHash_found`: tenant/user/refresh_token を persist し検索一致）
- [x] `RefreshTokenJpaRepositoryTest.java` に異常系のテストコードを記載する（`findByTokenHash_notFound`: 一致するレコードがない場合に empty が返る）

## 4. TenantJpaRepository の新規作成

- [x] `infrastructure/persistence/repository/TenantJpaRepository.java` を新規作成する（`JpaRepository<TenantJpaEntity, Long>`。カスタムクエリメソッドは追加しない。`getReferenceById` の利用のみが目的であることを Javadoc に明記する）
- [x] Javadoc を記載する

## 5. RefreshTokenMapper の新規作成

- [x] `infrastructure/persistence/mapper/RefreshTokenMapper.java` を新規作成する（`@Mapper(componentModel = "spring")`。`toDomain(RefreshTokenJpaEntity entity): RefreshToken`（`tenant.id` → `TenantId`、`user.id` → `UserId`、`tokenHash` → `TokenHash`、`expiresAt`（`OffsetDateTime`）→ `Instant` 変換、`revoked` はそのまま）。`toEntity(RefreshToken domain, TenantJpaEntity tenant, UserJpaEntity user): RefreshTokenJpaEntity`（`domain.id()` → `id`（`null` 許容）、引数の `tenant` / `user` をそのまま設定、`tokenHash` / `expiresAt`（`Instant` → `OffsetDateTime` 変換）/ `revoked` を設定、`createdBy` / `updatedBy` はいずれも `String.valueOf(domain.userId().value())`、`createdAt` / `updatedAt` はマッピング対象外として明示的に ignore する）
- [x] Javadoc を記載する
- [x] `RefreshTokenMapperTest.java` を新規作成し、`new RefreshTokenMapperImpl()` で正常系のテストコードを記載する（`toDomain_allFieldsMapped`・`toEntity_allFieldsMapped`（`createdBy` / `updatedBy` が `userId` の文字列化と一致することを含む））
- [x] `RefreshTokenMapperTest.java` に異常系のテストコードを記載する（`toDomain` で `tenant.id` / `user.id` / `tokenHash` が不正な場合に VO 生成の `IllegalArgumentException` が伝播することを確認する。`UserMapperTest` の異常系ケースに倣う）

## 6. RefreshTokenRepositoryImpl の新規作成・StubRefreshTokenRepository の削除

- [x] `infrastructure/persistence/repository/RefreshTokenRepositoryImpl.java` を新規作成する（`@Repository` + `@RequiredArgsConstructor`。フィールド: `RefreshTokenJpaRepository` / `RefreshTokenMapper` / `TenantJpaRepository` / `UserJpaRepository`。`findByTokenHash` は `refreshTokenJpaRepository.findByTokenHash(tokenHash.value()).map(refreshTokenMapper::toDomain)`。`save` は `tenantJpaRepository.getReferenceById(...)` / `userJpaRepository.getReferenceById(...)` で参照を解決し、`refreshTokenMapper.toEntity(...)` でエンティティを構築、`refreshTokenJpaRepository.save(entity)` の結果を `toDomain` で返す（詳細設計は requirements.md「設計判断」参照）
- [x] Javadoc を記載する（`save` の INSERT / UPDATE 両対応の挙動を明記する）
- [x] `infrastructure/persistence/repository/StubRefreshTokenRepository.java` を削除する
- [x] `RefreshTokenRepositoryImplTest.java` を新規作成し、`@DataJpaTest` + `TestEntityManager` で正常系のテストコードを記載する（`findByTokenHash_found`、`save_insertsNewToken`（`id` が `null` の `RefreshToken` を保存し、採番された `id` を含めて全フィールドが読み出せることを確認）、`save_updatesRevokedFlagWithoutOverwritingCreatedAt`（既存トークンを `revoke()` して保存し、`revoked` が更新される一方で `created_at` / `created_by` が元の値のまま変わらないことを確認））
- [x] `RefreshTokenRepositoryImplTest.java` に異常系のテストコードを記載する（`findByTokenHash_notFound`: 一致するレコードがない場合に empty が返る）

## 7. UserRepositoryImpl#findById の実装

- [x] `infrastructure/persistence/repository/UserRepositoryImpl.java` の `findById` を `userJpaRepository.findById(userId.value()).map(userMapper::toDomain)` へ置換し、`// TODO` コメントを削除する
- [x] Javadoc を更新する（スタブ表記を削除し、実装内容を記載する）
- [x] `UserJpaRepositoryTest.java` に正常系のテストコードを追加する（`findById_found`: 主キーで検索し一致するユーザーが返る）
- [x] `UserJpaRepositoryTest.java` に異常系のテストコードを追加する（`findById_notFound`: 存在しない主キーの場合に empty が返る）

## 8. SecureRandomRefreshTokenGenerator の新規作成・StubRefreshTokenGenerator の削除

- [x] `infrastructure/security/SecureRandomRefreshTokenGenerator.java` を新規作成する（`@Component`。`SecureRandom` で 256bit（32バイト）のランダムバイト列を生成し、`Base64.getUrlEncoder().withoutPadding()` で文字列化して `RawRefreshToken` を返す）
- [x] Javadoc を記載する
- [x] `infrastructure/security/StubRefreshTokenGenerator.java` を削除する
- [x] `SecureRandomRefreshTokenGeneratorTest.java` を新規作成し、コンストラクタで直接インスタンス化して正常系のテストコードを記載する（`generate_returnsNonBlankValue`、`generate_returnsDifferentValuesEachCall`）

## 9. Sha256RefreshTokenHasher の新規作成・StubRefreshTokenHasher の削除

- [x] `infrastructure/security/Sha256RefreshTokenHasher.java` を新規作成する（`@Component`。`MessageDigest.getInstance("SHA-256")` で入力文字列をハッシュ化し、64桁 hex 文字列化して `TokenHash` を返す）
- [x] Javadoc を記載する
- [x] `infrastructure/security/StubRefreshTokenHasher.java` を削除する
- [x] `Sha256RefreshTokenHasherTest.java` を新規作成し、コンストラクタで直接インスタンス化して正常系のテストコードを記載する（`hash_knownInputProducesExpectedSha256`: 既知の入力に対する SHA-256 の期待値と一致、`hash_sameInputProducesSameHash`）

## 10. JwtProperties 修正・JwtRefreshTokenExpirationPolicy の新規作成・StubRefreshTokenExpirationPolicy の削除

- [x] `infrastructure/security/JwtProperties.java` に `Duration refreshExpiration` フィールドを追加する（`jwt.refresh-expiration` にバインド）
- [x] `infrastructure/security/JwtRefreshTokenExpirationPolicy.java` を新規作成する（`@Component` + `@RequiredArgsConstructor`。`JwtProperties` をフィールドに持ち `expiration()` で `jwtProperties.refreshExpiration()` を返す）
- [x] Javadoc を記載する
- [x] `infrastructure/security/StubRefreshTokenExpirationPolicy.java` を削除する
- [x] `JwtRefreshTokenExpirationPolicyTest.java` を新規作成し、コンストラクタで直接インスタンス化して正常系のテストコードを記載する（`expiration_returnsValueFromJwtProperties`）

## 11. 設定ファイル修正

- [x] `src/test/resources/application-test.properties` に `jwt.refresh-expiration=14d` を追加する

## 12. SecurityConfig の TODO コメント削除

- [x] `config/SecurityConfig.java` の `// TODO: refreshエンドポイントの認可状態は暫定である` コメント行を削除する（`permitAll` 設定自体は変更しない）

## 13. TODO.md の更新

- [x] `infrastructure.mapper` 章に `RefreshTokenMapper#toDomain(RefreshTokenJpaEntity)` と `RefreshTokenMapper#toEntity(RefreshToken, TenantJpaEntity, UserJpaEntity)` を追記し、作成と同時に完成扱いとしてチェック済み（`[x]`）にする
- [x] `Repository (domain)` 章の `findById（UserRepository）` / `RefreshTokenRepository#findByTokenHash` / `RefreshTokenRepository#save` のチェックボックスを埋める
- [x] `Port (domain)` 章の `RefreshTokenGenerator#generate` / `RefreshTokenHasher#hash` / `RefreshTokenExpirationPolicy#expiration` のチェックボックスを埋める
- [x] `infrastructure.security` 章の `StubRefreshTokenGenerator を実装へ置換` / `StubRefreshTokenHasher を実装へ置換` / `StubRefreshTokenExpirationPolicy を実装へ置換` のチェックボックスを埋める
- [x] `infrastructure.persistence` 章の `StubRefreshTokenRepository を実装へ置換` / `UserRepositoryImpl#findById を実装` のチェックボックスを埋める

## 14. 全体テストの実施

- [x] 正常系のテストコードを記載する（上記の各対象を含む。漏れがないか一覧で確認する）
- [x] 異常系のテストコードを記載する（上記の各対象を含む。漏れがないか一覧で確認する）
