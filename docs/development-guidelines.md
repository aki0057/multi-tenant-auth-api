# 開発ガイドライン

## ドキュメント管理

### ドキュメントの分類

#### 1. 永続的ドキュメント（`docs/`）

アプリケーション全体の「何を作るか」や「どう作るか」を定義するドキュメント。
プロジェクトの全期間を通じて参照されるべきもので、頻繁に更新されないもの。

| ファイル                        | 内容                              |
|-----------------------------|---------------------------------|
| `architecture.md`           | 技術スタック、設計方針、技術的制約               |
| `repository-structure.md`   | フォルダ・ファイル構成、ディレクトリの役割、ファイル配置ルール |
| `database-design.md`        | ER図、テーブル定義、共通カラム方針              |
| `development-guidelines.md` | コーディング規約、テスト規約、Git規約（本ファイル）     |

#### 2. 作業単位のドキュメント（`.steering/[YYYYMMDD]-[作業内容]/`）

特定の開発作業における「今回何を作るか」を定義する一次的なステアリングファイル。
作業完了後は参照用として保持するが、新しい作業では新しいディレクトリを作成する。

| ファイル              | 内容                  |
|-------------------|---------------------|
| `requirements.md` | 変更・追加する機能の説明、制約事項   |
| `tasklist.md`     | 具体的な実装タスク、進捗状況、完了条件 |

---

## コーディング規約

### アノテーションプロセッサの順序

`pom.xml` の `annotationProcessorPaths` で **Lombok を MapStruct より先に宣言**する。

```xml
<annotationProcessorPaths>
    <path><!-- lombok --></path>
    <path><!-- mapstruct-processor --></path>
</annotationProcessorPaths>
```

MapStruct が Lombok の生成コード（setter / builder）を参照するため、宣言順序を逆にするとビルドエラーになる。

---

## テスト規約

### レイヤー別テスト方針

| 対象               | アノテーション                                                                                                                           | Spring 起動範囲    | モック対象                                |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------|----------------|--------------------------------------|
| Controller       | `@WebMvcTest(XxxController.class)` + `@Import({SecurityConfig.class, PasswordEncoderConfig.class, GlobalExceptionHandler.class})` | MVC レイヤーのみ     | Service → `@MockitoBean`             |
| ExceptionHandler | なし（`MockMvcBuilders.standaloneSetup()` で独立起動）                                                                                     | Spring 未起動     | 独自 `DummyController` で例外を発火          |
| Service          | `@ExtendWith(MockitoExtension.class)`                                                                                             | Spring 未起動     | Repository・PasswordEncoder → `@Mock` |
| Repository       | `@DataJpaTest`                                                                                                                    | JPA レイヤーのみ（H2） | なし（実 DB）                             |
| 結合テスト            | `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`                                                           | フルコンテキスト（H2）   | なし                                   |

### テストメソッド命名規則

```
methodName_condition_expectedResult()
```

- `login_success()`
- `login_userNotFound()`
- `findByTenantCodeAndEmail_sameEmailInDifferentTenants()`

`@DisplayName` は日本語で記載し、先頭に `正常系:` / `異常系:` / `境界値:` を付ける。

```java
@DisplayName("正常系: 正しい認証情報を送信すると 200 OK が返る。")
```

### 使用フレームワーク

| 用途       | ライブラリ                                                           |
|----------|-----------------------------------------------------------------|
| テストランナー  | JUnit 5 (Jupiter)                                               |
| モック      | Mockito（`@ExtendWith(MockitoExtension.class)` / `@MockitoBean`） |
| アサーション   | AssertJ                                                         |
| HTTP テスト | MockMvc                                                         |

### テストデータ管理

| レイヤー                 | 方法                                                                  |
|----------------------|---------------------------------------------------------------------|
| 結合テスト                | `@BeforeEach` / `@AfterEach` で `JdbcTemplate` により直接 INSERT / DELETE |
| Repository           | `TestEntityManager` で `persist()`                                   |
| Controller / Service | テストメソッド内または `@BeforeEach` のフィクスチャ                                   |

### テストプロファイル

`@ActiveProfiles("test")` を付けると `application-test.properties` が読み込まれ H2 インメモリ DB が使用される。
`@DataJpaTest` はデフォルトで組み込み DB に切り替わるため `@ActiveProfiles` は不要。

---

## Git 規約

### コミットメッセージ

#### フォーマット

```
<type>: <subject（日本語）>

* ファイル名.java: 実装内容の説明
* ファイル名.java: 実装内容の説明
```

- **1行目**: `<type>: <subject>` 形式。subject は日本語で記載する
- **2行目**: 必ず空行にする
- **3行目以降**: `*` で始まる箇条書きで実装内容を列挙する。形式は `* ファイル名: 説明`
- **スコープは使用しない**（`feat(auth):` のような記法は禁止）

> 2行目の空行は `gh pr create --fill` が subject を PR タイトル、3行目以降を PR 本文として自動認識するために必要。

#### prefix 一覧

| prefix     | 用途                         |
|------------|----------------------------|
| `feat`     | 新機能の追加                     |
| `fix`      | バグ修正                       |
| `docs`     | ドキュメントのみの変更                |
| `test`     | テストの追加・修正（プロダクションコードの変更なし） |
| `refactor` | バグ修正・機能追加を伴わないコード変更        |
| `chore`    | ビルド設定・依存関係・CI など雑務         |
| `style`    | コードスタイルのみの変更（ロジック変更なし）     |
| `perf`     | パフォーマンス改善                  |

#### サンプル

```
feat: ユーザーおよびテナントの永続化層（JPA）の実装とテストの追加

* UserRepository.java: テナントコードとメールアドレスでユーザーを検索するドメインインターフェースの追加
* TenantJpaEntity.java: テナント情報を表すJPAエンティティの追加
* UserJpaEntity.java: テナントと紐づくユーザー情報を表すJPAエンティティの追加
* UserMapper.java: UserJpaEntityからUserドメインモデルへ変換するMapStructマッパーの追加
* UserRepositoryImpl.java: UserRepositoryのJPA実装となるクラスの追加
```

```
test: ログインAPIの結合テストとテスト環境設定の追加

* LoginIntegrationTest.java: POST /login エンドポイントに対する正常系結合テストの追加
* application-test.properties: テスト用インメモリDB（H2）およびJPA設定の追加
```
