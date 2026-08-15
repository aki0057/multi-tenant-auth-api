# requirements

## 作業概要
admin エンドポイント（`GET /admin/users`。ログイン中の管理者と同一テナントのユーザー一覧）の永続化層を実装する。現在スタブ（`// TODO` + `List.of()` を返すだけ）である `infrastructure/persistence/repository/UserRepositoryImpl#findByTenantId(TenantId)` に実クエリを実装し、`UserRepository#findByTenantId` の Javadoc に定義済みの契約を満たす。

presentation 層（`AdminController` / `TenantUserResponse`、コミット b63971a）、`SecurityConfig` の `/admin/**` ADMIN 限定認可（`.steering/20260815-1733-config-security-admin-authorization/`）、application 層（`UserService#listTenantUsers`、コミット a0bcf6b・`.steering/20260815-1755-feat-userservice-list-tenant-users/`）はいずれも完了済みである。残るスタブは本 steering の対象 1 件のみであり、これを実装すると admin エンドポイントが端から端まで通る。

ドメインインターフェース `domain/repository/UserRepository#findByTenantId` は宣言済みで、その Javadoc が次の契約を定義している（**この契約を変更せず、実装で満たす**）。

1. 指定テナントに所属するユーザーを**すべて**返す。無効ユーザー（`users.is_active = false`）も除外しない。有効・無効の判定は呼び出し元の責務。
2. **ID 昇順**で返す（並び順の保証は本メソッドの責務）。
3. 該当ユーザーが存在しない場合は `null` ではなく**空リスト**を返す。

ユーザー決定事項（変更禁止）:

1. **取得手段**: `UserJpaRepository` に派生クエリメソッド `List<UserJpaEntity> findByTenant_IdOrderByIdAsc(Long tenantId)` を**追加**し、`UserRepositoryImpl#findByTenantId` はそれに委譲する。並び順（ID 昇順）は Java 側の `sorted()` ではなく **SQL の `ORDER BY id ASC`**（メソッド名の `OrderByIdAsc`）で保証する。
2. **N+1 対策**: 追加する派生クエリメソッドに `@EntityGraph(attributePaths = "tenant")` を付与する。`UserMapper#toDomain` が 1 件ごとに `tenant.id` / `tenant.code` / `tenant.active` を参照するため、`@ManyToOne(fetch = LAZY)` のままでは件数分の追加 SELECT が発生する。前 steering（`.steering/20260815-1755-feat-userservice-list-tenant-users/`）で「後続 steering で `@EntityGraph(attributePaths = "tenant")` による N+1 対策を実施する」と明記した決定を履行する。
3. **絞り込みは行わない**: `is_active` によるフィルタ条件を SQL にもコードにも入れない（無効ユーザー・無効テナントのユーザーも返す）。テナント越えアクセスの防止は WHERE 条件（`tenant_id` 一致）で担保する。
4. **ドメインモデルへの変換**: 既存 `UserMapper#toDomain` を 1 件ずつ適用し、`stream().map(userMapper::toDomain).toList()` で `List<User>` を組み立てる。Repository が返す順序をそのまま維持する（`sorted()` を挟まない）。
5. **スタブ痕跡の除去**: 既存の `// TODO` コメントと「現時点では未実装のスタブであり、常に空リストを返す。」という Javadoc 記述を削除する。
6. **既存メソッドは変更しない**: `findByTenantCodeAndEmail` / `findById`、および `UserJpaRepository#findByTenant_CodeAndEmail` は変更しない。

**単一レイヤー原則**: 作業対象は infrastructure(persistence) の Repository 実装 1 メソッドのみ。同一パッケージの Spring Data JPA インターフェース（`UserJpaRepository`）への派生クエリ追加は、その実装手段として同一レイヤー・同一種別に含める（`docs/testing-guidelines.md` でも「リポジトリ実装（`@Repository`）/ Spring Data JPA」は 1 つの種別として扱われる）。

変更してはならない既存ファイル:
- `domain/repository/UserRepository`（`findByTenantId` の宣言・Javadoc の契約を変更しない）
- `domain/model/User`、`domain/model/vo/*`
- `infrastructure/persistence/mapper/UserMapper`、`infrastructure/persistence/entity/UserJpaEntity`、`infrastructure/persistence/entity/TenantJpaEntity`
- application 層（`UserService` / `ListTenantUsersCommand` / `TenantUserResult`）、presentation 層すべて、`config/` すべて
- 既存テストの既存テストケース（`UserJpaRepositoryTest` の既存メソッド・ビルダーは変更せず、追記のみ）

スコープ外（後続の別 steering で実施する）:
- 結合テスト `AdminIntegrationTest`（`@SpringBootTest` によるエンドツーエンド検証）。`LoginIntegrationTest` / `LogoutIntegrationTest` と同様に結合テスト単独の steering として切り出す（`.steering/20260720-1156-test-logout-integration/` の前例に倣う）。
- ページネーション・検索条件の追加。

## 作業対象レイヤー
infrastructure（`infrastructure/persistence` の Repository 実装）

## 作業対象の種別
Repository（domain の `UserRepository` インターフェースに対する infrastructure 側の実装。判定早見表の「Repository (domain)」行を適用する）

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 (Jupiter)。`docs/testing-guidelines.md` の「リポジトリ実装（`@Repository`）/ Spring Data JPA」に従い `@DataJpaTest`（Spring は JPA レイヤーのみ起動、H2 実 DB）。モックは使用しない。アサーションは AssertJ（`assertThat`）。テストデータは `TestEntityManager#persist` で用意する（`@ActiveProfiles` は不要）。
  - 新規作成: `src/test/java/io/github/aki0057/multitenant/auth/infrastructure/persistence/repository/UserRepositoryImplTest.java`。`RefreshTokenRepositoryImplTest` と同じ流儀とし、`UserJpaRepository` は `@Autowired` で取得、`UserRepositoryImpl` は `@BeforeEach` で `new UserRepositoryImpl(userJpaRepository, new UserMapperImpl())` と手動配線する（`UserMapperImpl` は MapStruct 生成クラス）。
  - 追記: 既存 `src/test/java/io/github/aki0057/multitenant/auth/infrastructure/persistence/repository/UserJpaRepositoryTest.java` に、新規派生クエリ（`findByTenant_IdOrderByIdAsc`）のケースを追記する。既存の `buildTenant` / `buildUser` ビルダーを流用し、必要に応じて `active` / `role` を差し替えられる補助ビルダーを Javadoc 付きで追加してよい。
  - 命名は `methodName_condition()`、`@DisplayName` は日本語で先頭に `正常系:` / `異常系:` を付ける。既存 2 クラスに倣い `// ---` のセクション区切りコメントを用いる。
- Spring MVC アノテーション: なし（infrastructure 層のため）。使用する Spring アノテーションは既存どおり `@Repository`（`UserRepositoryImpl`）・Lombok の `@RequiredArgsConstructor` と、新規に `org.springframework.data.jpa.repository.EntityGraph`（`UserJpaRepository` の派生クエリ）。

## 隣接レイヤー
- 1 つ外側のレイヤー: domain（`domain/repository/UserRepository#findByTenantId`）— 既に存在する（宣言・Javadoc の契約とも確定済み）
- 1 つ内側のレイヤー: なし（Spring Data JPA / H2 という技術基盤に到達する。同一レイヤー内の `UserJpaRepository`・`UserMapper` は既存のものを利用する）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる。

呼び出し元（実在コード）: `domain/repository/UserRepository`

```java
/**
 * 指定されたテナントに所属するユーザーを検索する。
 * （該当テナントのユーザーをすべて／無効ユーザーも除外しない／ID 昇順／該当なしは空リスト）
 */
List<User> findByTenantId(TenantId tenantId);
```

呼び出しの実体は `application/UserService#listTenantUsers` の `userRepository.findByTenantId(tenantId)`（コミット a0bcf6b）であり、返却された順序をそのまま維持して `TenantUserResult` へ詰め替える。したがって並び順（ID 昇順）の保証責務は本作業対象にある。

- メソッド名: `findByTenantId`
- 引数: `TenantId tenantId`（`record TenantId(Long value)`。`null` / 0 以下は VO 側で `IllegalArgumentException`。よって実装到達時点で 1 以上の値が保証される）
- 戻り値: `List<User>`（`null` を返さない。0 件は空リスト。ID 昇順）

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド。既存スタブのシグネチャ・`@Override` を変更せず、本体のみを実装する。

- メソッド名: `findByTenantId`（`io.github.aki0057.multitenant.auth.infrastructure.persistence.repository.UserRepositoryImpl`）
- 引数: `TenantId tenantId`
- 戻り値: `List<User>`

実装方針:

```java
@Override
public List<User> findByTenantId(TenantId tenantId) {
    return userJpaRepository.findByTenant_IdOrderByIdAsc(tenantId.value())
            .stream()
            .map(userMapper::toDomain)
            .toList();
}
```

あわせて `UserJpaRepository` へ次を追加する（同一レイヤー・同一種別の実装手段）。

```java
@EntityGraph(attributePaths = "tenant")
List<UserJpaEntity> findByTenant_IdOrderByIdAsc(Long tenantId);
```

- 派生クエリ名の `Tenant_Id` は `@ManyToOne` の `tenant` の主キーを指す（既存 `findByTenant_CodeAndEmail` と同じ命名規約）。
- `OrderByIdAsc` により `ORDER BY id ASC` が SQL に含まれる。
- `@EntityGraph(attributePaths = "tenant")` により `tenant` を JOIN FETCH し、`UserMapper#toDomain` の `tenant.id` / `tenant.code` / `tenant.active` 参照で N+1 が発生しないようにする。
- `java.util.List` / `org.springframework.data.jpa.repository.EntityGraph` の import を追加する。既存 `findByTenant_CodeAndEmail` は変更しない。

### 期待する挙動（受け入れ条件）
- 指定テナントに所属するユーザーが **ID 昇順**の `List<User>` で返る。
- `users.is_active = false` のユーザーも除外されず含まれ、`User#userIdIsActive()` が `false` になる。
- テナントが無効（`tenants.is_active = false`）でも、そのテナントのユーザーは返る（`User#tenantIdIsActive()` が `false` になるだけ）。
- 他テナントのユーザーは 1 件も含まれない（同一メールアドレスが別テナントに存在しても混在しない）。
- 該当ユーザーが 0 件のテナント、および存在しないテナント ID の場合は `null` ではなく空リストを返す（例外をスローしない）。
- 各 `User` のフィールドが `UserMapper#toDomain` の規約どおり変換される（`userId` / `tenantId` / `tenantCode` / `email` / `passwordHash` / `role` / `userIdIsActive` / `tenantIdIsActive`）。
- `findByTenantCodeAndEmail` / `findById` の既存挙動は一切変わらない。

### Javadoc 方針
`UserRepositoryImpl#findByTenantId` の既存 Javadoc を次のとおり書き換える（スタブ記述の削除）。

- 「現時点では未実装のスタブであり、常に空リストを返す。」を削除し、`UserJpaRepository#findByTenant_IdOrderByIdAsc` へ委譲して `UserMapper` でドメインモデルへ変換する旨を記す（既存 `findById` / `RefreshTokenRepositoryImpl` の記述スタイルに合わせる）。
- 並び順（ID 昇順）は SQL の `ORDER BY` で保証し、Java 側で並び替えないことを明記する。
- 無効ユーザー・無効テナントのユーザーも除外しないこと（有効性判定は呼び出し元の責務）を明記する。
- `@EntityGraph` により `tenant` を JOIN FETCH して N+1 を回避していることを明記する。
- `@param tenantId` / `@return`（該当なしの場合は空リスト）を記載する。

`UserJpaRepository#findByTenant_IdOrderByIdAsc` にも Javadoc を付ける（メソッド名から Spring Data JPA が `WHERE tenant_id = ? ORDER BY id ASC` を自動生成すること・`@EntityGraph` の意図・戻り値が空リストになりうること）。クラス Javadoc の「テナントコードとメールアドレスで users テーブルを検索する。」も、テナント ID 検索を含む記述へ更新してよい。

新規テストクラス `UserRepositoryImplTest` にはクラス Javadoc を付ける（`RefreshTokenRepositoryImplTest` に倣い、`@DataJpaTest` で H2 を起動し `UserMapperImpl` / `UserRepositoryImpl` を手動配線する旨）。

### テスト仕様の詳細
#### 1. `UserRepositoryImplTest`（新規・主対象）
`@DataJpaTest`。`@BeforeEach` で `repository = new UserRepositoryImpl(userJpaRepository, new UserMapperImpl());`。テストデータは `TestEntityManager#persist` + `em.flush()`（必要に応じて `em.clear()`）で用意する。

正常系:
- `findByTenantId_found()` — 同一テナントに複数ユーザーを登録し、全件が返ること・各フィールド（`userId` / `tenantId` / `tenantCode` / `email` / `role` / `userIdIsActive` / `tenantIdIsActive`）が正しく変換されることを検証する。
- `findByTenantId_orderedByIdAsc()` — 永続化順とメールアドレスの辞書順が一致しないようにデータを投入し、返却順が `userId` の昇順であること（`extracting(u -> u.userId().value()).isSorted()` 等）を検証する。
- `findByTenantId_includesInactiveUser()` — `is_active = false` のユーザーが除外されず含まれ、`userIdIsActive()` が `false` であることを検証する。
- `findByTenantId_inactiveTenant()` — テナントが無効でもユーザーが返り、`tenantIdIsActive()` が `false`・`userIdIsActive()` は `users.is_active` の値であることを検証する。
- `findByTenantId_otherTenantExcluded()` — 2 テナントにユーザーを登録し（同一メールアドレスを含む）、指定テナントのユーザーのみが返り、他テナントのユーザーが 1 件も含まれないことを検証する。

異常系:
- `findByTenantId_noUsers()` — ユーザーが 1 人も所属しないテナントを指定した場合、`null` ではなく空リストが返ること。
- `findByTenantId_unknownTenantId()` — 存在しないテナント ID（例: `new TenantId(Long.MAX_VALUE)`）を指定した場合、例外をスローせず空リストが返ること。

補足: `TenantId` は `null` / 0 以下を VO 生成時に `IllegalArgumentException` で弾くため、それらを引数に取るケースは本 Repository のテスト対象外（`UserServiceTest#listTenantUsers_invalidTenantId` / `listTenantUsers_nullTenantId` で検証済み）。テスト内でこの点をコメントとして残してよい。

#### 2. `UserJpaRepositoryTest`（既存へ追記）
新規派生クエリのクエリ挙動を JPA レイヤーで直接検証する（`RefreshTokenJpaRepositoryTest` と `RefreshTokenRepositoryImplTest` が併存する構成に倣う）。既存テストケース・既存ビルダーは変更しない。

正常系:
- `findByTenant_IdOrderByIdAsc_found()` — 指定テナントのユーザーが ID 昇順で返り、他テナントのユーザーが含まれないこと。
- `findByTenant_IdOrderByIdAsc_includesInactiveUser()` — `is_active = false` のユーザーも返ること（クエリに `is_active` の絞り込みが入っていないこと）。

異常系:
- `findByTenant_IdOrderByIdAsc_notFound()` — 該当ユーザーが存在しないテナント ID の場合、空リストが返ること。

`active` を差し替えられるようにするため、既存 `buildUser(TenantJpaEntity, String)` を変更せず、オーバーロード（例: `buildUser(TenantJpaEntity tenant, String email, boolean active)`）を Javadoc 付きで追加してよい。無効テナントを作る場合も同様に `buildTenant` のオーバーロードを追加してよい。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
該当なし（作業対象は Repository のため対象外。内側スタブの新規作成・`TODO.md` への登録は発生しない）。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
該当なし（作業対象は Repository のため対象外）。

参考として、本作業で利用する既存のドメインモデルは次のとおり（いずれも新規作成・変更なし）。
- `domain/model/vo/TenantId`（引数。`value()` で `Long` を取り出して Spring Data JPA へ渡す。プリミティブ化は infrastructure 境界内に閉じる）
- `domain/model/User`（戻り値の要素。生成は `UserMapper#toDomain` が担う）
