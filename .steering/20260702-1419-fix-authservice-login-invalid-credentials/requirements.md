# requirements

## 作業概要

`AuthService#login(LoginCommand command)` の入口で行っている 3 つの Value Object 生成（`new TenantCode(command.tenantCode())` / `new Email(command.email())` / `new RawPassword(command.password())`）は、フィールドは存在するが形式が不正な入力（例: tenantCode に記号を含む、email が `@` 直後にドットを持たない、password が 8 文字未満）に対して `IllegalArgumentException` を投げる。現状これを AuthService は捕捉していないため、`GlobalExceptionHandler` の `handleUnexpected`（`Exception.class` ハンドラ）に落ち、HTTP 500 として応答してしまう。

本タスクでは、この 3 つの VO 生成を try/catch で囲み、捕捉した `IllegalArgumentException` を `BadCredentialsException("Invalid credentials")` に変換してスローするよう `AuthService#login` を改修する。これにより「フィールドは存在するが形式が不正」な資格情報も、ユーザー不在・パスワード不一致と同様に HTTP 401 として応答されるようになる。

変換後の例外は既存の `GlobalExceptionHandler#handleBadCredentials`（`BadCredentialsException` → 401、`{"error": "Invalid credentials"}`）でそのまま処理される。presentation 層への変更（新規例外ハンドラの追加等）は行わない。また `LoginRequest`（DTO）への `@Pattern` / `@Size` 等のバリデーションアノテーション追加も行わない（パスワード形式要件を DTO 経由で露出させず、VO → 401 への変換に寄せる方針のため）。

本作業は新規レイヤー作成ではなく、既に完成している `AuthService#login` の異常系ハンドリングを堅牢化するものである。呼び出す内側の依存（`UserRepository` / `PasswordVerifier` / `AccessTokenProvider` / `TenantCode` / `Email` / `RawPassword`）はすべて既存の完成済みコードであり、変更しない。新規スタブ・新規 DomainObject・新規 ValueObject の作成は発生しない。

## 作業対象レイヤー

application (Service)

## 作業対象の種別

Service

## 使用するテスト・フレームワーク等

- テストフレームワーク:
  - `AuthServiceTest`: JUnit5 + Mockito（`@ExtendWith(MockitoExtension.class)`、`@Mock`、`@InjectMocks`）によるユニットテスト。
  - `LoginIntegrationTest`: JUnit5 + `@SpringBootTest` + `@AutoConfigureMockMvc`（MockMvc 経由の e2e テスト）。
- Spring MVC アノテーション: 該当なし（本タスクは presentation 層に変更を加えないため、新規の Spring MVC アノテーションは使用しない）。

## 隣接レイヤー

- 1 つ外側のレイヤー: presentation (`AuthController#login`)
- 1 つ内側のレイヤー: domain (`TenantCode` / `Email` / `RawPassword` の各 Value Object。いずれも実装済みで変更しない)

## 外側レイヤーとの契約

外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる（変更禁止）。

`AuthController#login` が以下のように呼び出している。

```java
String accessToken = authService.login(new LoginCommand(
        request.getTenantCode(),
        request.getEmail(),
        request.getPassword()
));
```

- メソッド名: `login`
- 引数: `LoginCommand command`
- 戻り値: `String`

## 作業対象メソッドのシグネチャ

作業概要を達成するための単一メソッド（シグネチャ変更なし。メソッド本体の入口部分のみ改修する）。

- メソッド名: `login`
- 引数: `LoginCommand command`
- 戻り値: `String`

```java
@Transactional(readOnly = true)
public String login(@NonNull LoginCommand command)
```

改修方針（実装の詳細は段階3 implementer が行う。ここでは方針のみ記す）:

```java
final TenantCode tenantCode;
final Email email;
final RawPassword rawPassword;
try {
    tenantCode = new TenantCode(command.tenantCode());
    email = new Email(command.email());
    rawPassword = new RawPassword(command.password());
} catch (IllegalArgumentException e) {
    // VO の形式検証失敗を、認証 API の共通レスポンス（401）へ変換する
    throw new BadCredentialsException("Invalid credentials");
}
```

- 例外の変換元: `TenantCode` / `Email` / `RawPassword` のコンパクトコンストラクタが投げる `java.lang.IllegalArgumentException`
- 例外の変換先: `org.springframework.security.authentication.BadCredentialsException("Invalid credentials")`（AuthService 内で既に import 済み・既存の「ユーザー不在」「認証失敗」ケースと同一の例外型）
- 変換後の HTTP レスポンス: 既存の `GlobalExceptionHandler#handleBadCredentials` により 401 Unauthorized（変更不要・そのまま再利用）

## 内側レイヤーへの契約

該当なし。本タスクは新規の内側呼び出し・新規スタブを一切作成しない。

`AuthService#login` が呼び出す内側の依存（`UserRepository#findByTenantCodeAndEmail`、`PasswordVerifier`、`AccessTokenProvider#issue`、および `TenantCode` / `Email` / `RawPassword` の各コンストラクタ）はすべて既存の完成済みコードであり、シグネチャ・実装ともに変更しない。したがって「内側レイヤーはまだ存在しないためスタブを新規作成する」という前提は本タスクには適用されない。

## ドメインモデルの利用

プリミティブ型への依存を禁止する。`LoginCommand` が保持するプリミティブ（`tenantCode` / `email` / `password`）は、Service の入口（`login` メソッド冒頭）で既存の `TenantCode` / `Email` / `RawPassword` Value Object へ変換する（この変換ロジック自体は既存実装であり、本タスクではその変換に失敗した場合の例外処理のみを追加する）。

- 作成する DomainObject / ValueObject 名: なし（新規作成なし。既存の `TenantCode` / `Email` / `RawPassword` をそのまま利用する）
