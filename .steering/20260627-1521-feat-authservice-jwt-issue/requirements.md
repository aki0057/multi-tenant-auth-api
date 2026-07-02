# requirements

## 作業概要

`AuthService#login(LoginCommand command)` の末尾にある `// TODO: 認証済み user から JWT アクセストークンを発行して返す` および `return "";` を、domain 層に新規作成する `AccessTokenProvider` インターフェース（スタブ）の `issue(user)` 呼び出しに置き換える。
認証検証ロジック（TenantCode/Email による UserRepository 検索・active 確認・PasswordEncoder によるパスワード照合・失敗時 BadCredentialsException）は既存のままとし、シグネチャも変更しない。

加えて、`@SpringBootTest` フルコンテキストテストが DI コンテナで具象 Bean を解決できるよう、`AccessTokenProvider` を実装する一時スタブアダプタ（`StubAccessTokenProvider`）を infrastructure 層に新規作成する（下記「改訂経緯」および「追加スタブアダプタ」を参照）。

## 改訂経緯（段階4 FAIL によるユーザー承認済み差し戻し）

段階4（tester）が FAIL した原因: `@SpringBootTest` フルコンテキストテスト（`LoginIntegrationTest`、`MultiTenantAuthApiApplicationTests.contextLoads`）が、`AccessTokenProvider` の具象 Bean が DI コンテナに登録されていないため context 起動に失敗した。

ユーザーが改訂を承認した方針: `AccessTokenProvider` を implements する一時スタブアダプタ（Spring `@Component`）を本増分に追加し、context 起動を可能にする。

## 作業対象レイヤー

application (Service)

（単一レイヤー原則との整合注記: 本来この増分は application(Service) 単体の作業であるが、domain 層への `AccessTokenProvider` インターフェース（スタブ）および infrastructure 層への `StubAccessTokenProvider`（一時スタブアダプタ）を伴う。どちらも `docs/file-change-workflow.md` の「スタブの定義」に沿う「コンパイル／コンテキスト起動を通すためだけのクラス」であり、ユーザーが明示的に承認した方針である。plan-verifier はこれを単一レイヤー違反として差し戻さないこと。）

## 作業対象の種別

Service

## 使用するテスト・フレームワーク等

- テストフレームワーク: JUnit5 + Mockito（`@ExtendWith(MockitoExtension.class)`、`@Mock`、`@InjectMocks`）
- Spring MVC アノテーション: なし（AuthServiceTest は Spring コンテキストを使わない純粋な Mockito テスト）

## 隣接レイヤー

- 1 つ外側のレイヤー: presentation (AuthController)
- 1 つ内側のレイヤー: domain (AccessTokenProvider インターフェース・スタブ) ← infrastructure (StubAccessTokenProvider) がこのインターフェースを実装する

## 外側レイヤーとの契約

外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる。

`AuthController#login` が以下のように呼び出している（変更禁止）。

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

作業概要を達成するための単一メソッド（シグネチャ変更なし）。

- メソッド名: `login`
- 引数: `LoginCommand command`
- 戻り値: `String`

```java
@Transactional(readOnly = true)
public String login(LoginCommand command)
```

## 内側レイヤーへの契約

### AccessTokenProvider インターフェース（domain 層スタブ）

内側はまだ存在しないため、スタブとして新規作成する（実装しない・TODO のみ）。

- インターフェース名: `AccessTokenProvider`
- 配置パッケージ: `io.github.aki0057.multitenant.auth.domain.service`
- ファイルパス: `src/main/java/io/github/aki0057/multitenant/auth/domain/service/AccessTokenProvider.java`
- メソッド名: `issue`
- 引数: `User user`（`io.github.aki0057.multitenant.auth.domain.model.User`）
- 戻り値: `String`
- スタブ方針: interface 宣言のみとし、クラスに `// TODO` 目印を付ける。実装クラスは作成しない。

**TODO.md 登録先:** `## Port (domain)` 章（既に存在する場合はそのまま利用し重複登録しない）。

## 追加スタブアダプタ（infrastructure 層・ユーザー承認済み）

`AccessTokenProvider` を実装する一時スタブアダプタを infrastructure 層に新規作成する。

これは `docs/file-change-workflow.md` の「スタブの定義」に沿うものである（コンパイル／コンテキスト起動を通すためだけに作成するクラス・中身の具体的な処理を実装しない・`// TODO` 目印を付ける・実際の実装は後続タスクで行う）。

追加理由: `LoginIntegrationTest`・`MultiTenantAuthApiApplicationTests.contextLoads` の `@SpringBootTest` が `AccessTokenProvider` の具象 Bean を DI コンテナに要求する。スタブアダプタを登録することで context 起動を可能にし、既存フルコンテキスト統合テストを green に保つ。本物の jjwt 署名・claim 構築ロジックは実装しない・考えない。後続の infrastructure 増分で `StubAccessTokenProvider` を jjwt 実装クラスへ置換する。

- クラス名: `StubAccessTokenProvider`
- 配置パッケージ: `io.github.aki0057.multitenant.auth.infrastructure.security`
- ファイルパス: `src/main/java/io/github/aki0057/multitenant/auth/infrastructure/security/StubAccessTokenProvider.java`
- Spring アノテーション: `@Component`
- 実装インターフェース: `AccessTokenProvider`（`io.github.aki0057.multitenant.auth.domain.service.AccessTokenProvider`）
- `issue(User user)` の実装: `return "";`（ダミー値のみ。jjwt 処理なし）
- スタブ目印: クラスに `// TODO: jjwt 実装へ置換（後続 infrastructure 増分）` を付与する

**TODO.md 登録先:** `## infrastructure.security` 章を新設し（`## infrastructure.mapper` の命名パターンに整合）、`StubAccessTokenProvider を jjwt 実装へ置換` を空チェックボックスで追記する。`## Port (domain)` の `AccessTokenProvider#issue` とは別管理とし重複起票しない。

## ドメインモデルの利用

プリミティブ型への依存を禁止する。本作業では新規 DomainObject / ValueObject の作成はなく、認証済みの既存 `User` ドメインオブジェクト（`io.github.aki0057.multitenant.auth.domain.model.User`）をそのまま `AccessTokenProvider#issue` の引数として渡す。

- 作成する DomainObject / ValueObject 名: なし（新規作成なし。既存の `User` をそのまま利用する）
