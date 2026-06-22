# テスト規約

## 前提

- テストランナー: JUnit 5 (Jupiter)
- アサーション: AssertJ。`assertThat(...)` で統一する。

モック（Mockito）・HTTP テスト（MockMvc）の使い分けは「レイヤー別テスト方針」を参照する。

## レイヤー別テスト方針

テスト方針は [repository-structure.md](repository-structure.md) の「ファイル配置ルール」の種別と対応させる。種別ごとに、テストを作成するもの（①）と作成しないもの（②）に分ける。

### ① テストを作成する種別

| 種別                                      | 配置先                                      | テスト方法（アノテーション・起動）                                                                                                                 | Spring 起動範囲    | モック対象                                |
|-----------------------------------------|------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|----------------|--------------------------------------|
| Controller（`@RestController`）           | `presentation/`                          | `@WebMvcTest(XxxController.class)` + `@Import({SecurityConfig.class, PasswordEncoderConfig.class, GlobalExceptionHandler.class})` | MVC レイヤーのみ     | Service → `@MockitoBean`             |
| 例外ハンドラー（`@RestControllerAdvice`）        | `presentation/advice/`                   | なし（`MockMvcBuilders.standaloneSetup()` で独立起動）                                                                                     | Spring 未起動     | 独自 `DummyController` で例外を発火          |
| JWT フィルター                               | `presentation/filter/`                   | `@ExtendWith(MockitoExtension.class)`（`MockHttpServletRequest` / `MockHttpServletResponse` / `MockFilterChain` を使用）               | Spring 未起動     | `JwtService` 等 → `@Mock`             |
| UseCase / Service（`@Service`）           | `application/`                           | `@ExtendWith(MockitoExtension.class)`                                                                                             | Spring 未起動     | Repository・PasswordEncoder → `@Mock` |
| Domain オブジェクト / ValueObject             | `domain/model/`（vo は `domain/model/vo/`） | なし（プレーンな JUnit）                                                                                                                   | Spring 未起動     | なし                                   |
| リポジトリ実装（`@Repository`）/ Spring Data JPA | `infrastructure/persistence/repository/` | `@DataJpaTest`                                                                                                                    | JPA レイヤーのみ（H2） | なし（実 DB）                             |
| MapStruct マッパー                          | `infrastructure/persistence/mapper/`     | なし（`Mappers.getMapper(XxxMapper.class)` でインスタンス化）                                                                                 | Spring 未起動     | なし                                   |
| 結合テスト（横断）                               | —                                        | `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`                                                           | フルコンテキスト（H2）   | なし                                   |

### ② テストを作成しない種別

| 種別                             | 配置先                                  | 理由                                                        |
|--------------------------------|--------------------------------------|-----------------------------------------------------------|
| Request / Response DTO         | `presentation/`                      | バリデーション等は Controller テスト（`@WebMvcTest`）で検証する              |
| Command オブジェクト                 | `application/`                       | ロジックを持たない record。生成と同時に完成扱い（`file-change-workflow.md` 参照） |
| リポジトリインターフェース                  | `domain/repository/`                 | インターフェースのみ。実装（`@DataJpaTest`）で検証する                        |
| JPA エンティティ（`@Entity`）          | `infrastructure/persistence/entity/` | データ保持のみ。Repository テストで間接的に検証する                           |
| Spring 設定クラス（`@Configuration`） | `config/`                            | 結合テストで間接的に検証する                                            |

## テストメソッド命名規則

```
methodName_condition()
```

2部構成とする。2番目の要素には、そのケースを識別する条件または期待結果を記述する。

- `login_success()`
- `login_userNotFound()`
- `findByTenantCodeAndEmail_sameEmailInDifferentTenants()`

`@DisplayName` は日本語で記載し、先頭に `正常系:` / `異常系:`を付ける。

```java
@DisplayName("正常系: 正しい認証情報を送信すると 200 OK が返る。")
```

## テストデータ管理

| 種別                                           | テストデータの用意                                                           |
|----------------------------------------------|---------------------------------------------------------------------|
| 結合テスト（横断）                                    | `@BeforeEach` / `@AfterEach` で `JdbcTemplate` により直接 INSERT / DELETE |
| リポジトリ実装 / Spring Data JPA                    | `TestEntityManager` で `persist()`                                   |
| Controller / UseCase・Service                 | テストメソッド内または `@BeforeEach` のフィクスチャ（モックの戻り値として用意）                     |
| Domain オブジェクト / ValueObject / MapStruct マッパー | テストメソッド内で直接生成する                                                     |

## テストプロファイル

`@ActiveProfiles("test")` を付けると `application-test.properties` が読み込まれ H2 インメモリ DB が使用される。
`@DataJpaTest` はデフォルトで組み込み DB に切り替わるため `@ActiveProfiles` は不要。
