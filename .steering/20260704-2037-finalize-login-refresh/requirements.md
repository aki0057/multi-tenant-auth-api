# requirements

## 作業概要
2 パス移行の Pass 2（presentation ＋ 移行ブリッジ撤去）。login ユースケースを「アクセストークン単発発行」から「アクセストークン＋リフレッシュトークン発行」へ最終統合する。具体的には次を行う。

- presentation: `LoginResponse` を `RefreshResponse` と同形の 3 引数 `(accessToken, refreshToken, tokenType)` へ拡張し、`AuthController#login` を `AuthService#login(LoginCommand): LoginResult` の呼び出しへ切り替える。
- 内側契約の確定（移行ブリッジ撤去）: Pass 1 で一時的に残していた旧シム `AuthService#login(LoginCommand): String` を削除し、一時名 `AuthService#loginWithRefreshToken(LoginCommand): LoginResult` を `login` へリネームする。これにより最終形 `AuthService#login(LoginCommand) -> LoginResult` に一本化する。

本タスクは新規機能追加ではなく、Pass 1 で用意済みのリフレッシュトークン発行版ロジック（`loginWithRefreshToken`）を最終名 `login` に昇格し、旧シムと一時名を撤去する移行仕上げである。`AuthService#login` 本体のロジック（認証・リフレッシュトークン新規発行・保存）は Pass 1 で完成済みであり変更しない。

## 作業対象レイヤー
presentation (API / Controller)

## 作業対象の種別
API (presentation)

## 使用するテスト・フレームワーク等
- テストフレームワーク:
  - presentation: JUnit ＋ Spring MVC（`@WebMvcTest` / MockMvc、`AuthService` は `@MockitoBean`）、結合は `@SpringBootTest` ＋ `@AutoConfigureMockMvc`（H2・`@ActiveProfiles("test")`）
  - application（テスト追随のみ）: JUnit ＋ Mockito（`@ExtendWith(MockitoExtension.class)`）
- Spring MVC アノテーション: `@RestController` / `@PostMapping("/login")` / `@Valid` / `@RequestBody`（既存を踏襲、変更しない）

## 隣接レイヤー
- 1 つ外側のレイヤー: なし（API は最も外側。呼び出し元は外部 HTTP クライアント）
- 1 つ内側のレイヤー: application (AuthService)

## 外側レイヤーとの契約
（条件に非該当: 作業対象が API のため記載しない）

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド（presentation）。
- メソッド名: `AuthController#login`
- 引数: `@Valid @RequestBody LoginRequest request`（変更なし）
- 戻り値: `ResponseEntity<LoginResponse>`。`LoginResponse` を 3 引数 `(String accessToken, String refreshToken, String tokenType)` へ変更する。返却は `ResponseEntity.ok(new LoginResponse(result.accessToken(), result.refreshToken(), "Bearer"))`

補足（同一 presentation ファイルの変更）:
- `LoginResponse` を `public record LoginResponse(String accessToken, String tokenType)` から `public record LoginResponse(String accessToken, String refreshToken, String tokenType)` へ拡張し、Javadoc（`@param`）を `RefreshResponse` に揃える。

## 内側レイヤーへの契約
本タスクは移行仕上げのため、内側（application）のメソッドは新規スタブ作成ではなく「Pass 1 で作成済みの一時名メソッドの昇格 ＋ 旧シム撤去」を行う。最終契約は次に確定する。
- メソッド名: `AuthService#login`（`loginWithRefreshToken` からのリネーム）
- 引数: `@NonNull LoginCommand command`
- 戻り値: `LoginResult`（`record LoginResult(String accessToken, String refreshToken)`、既存）
- 実施内容:
  - 旧 `public String login(@NonNull LoginCommand command)`（`@Transactional(readOnly = true)`、現 61-87 行）を削除する。
  - `public LoginResult loginWithRefreshToken(@NonNull LoginCommand command)`（`@Transactional`、現 116-152 行）を `login` へリネームする（本体ロジックは不変）。
  - リネーム後 `login` の Javadoc から「2 パス移行のための一時名メソッドである…」段落（現 105-108 行付近）を撤去し、通常の login 説明へ整える。
- 参照確認（grep 済み）: 旧 `String login(LoginCommand)` の production 参照は `AuthController#login`（現 29 行）のみ。`loginWithRefreshToken` の参照は `AuthService` 自身とテストのみ。テスト側の参照は下記「テストへの影響」で追随する。

補足（Command の扱い）: `LoginCommand` は既存の record であり新規作成・変更なし。`TODO.md` にも登録しない（作成済み・完成扱い）。

## ドメインモデルの利用
（条件に非該当: 作業対象が API のため記載しない。プリミティブ → ValueObject 変換は既存の `AuthService#login` 本体で完了済みで変更しない）

## テストへの影響（新シグネチャに伴い追随が必要なもの）
- `presentation/AuthControllerTest`:
  - `login_success`: `when(authService.login(any())).thenReturn("mock-access-token")` を `LoginResult` 返却へ変更し、レスポンス JSON に `$.refreshToken` を追加検証する。
  - `login_invalidCredentials` / `login_unexpectedException` / `login_invalidRequest`: `doThrow(...).when(authService).login(any())` はコンパイル互換だが新シグネチャ整合を確認する。
- `LoginIntegrationTest`（ルートパッケージ配下）: 正常系は現状 `status().isOk()` のみ。新レスポンス形（`accessToken` / `refreshToken` / `tokenType`）を検証する形へ追随する。異常系は挙動不変だが整合を確認する。
- `application/AuthServiceTest`:
  - 旧 `String login(LoginCommand)` を対象とする `login_*` テスト群（現 89-163 行）は、旧シム削除に伴い撤去する。
  - `loginWithRefreshToken_*` テスト群（現 176-289 行）は、リネーム後 `authService.login(...)` 呼び出しへ追随する（一時名の参照・`DisplayName`／コメント整備を含む）。

## TODO.md 整合
- `TODO.md:13`「AuthService#login(LoginCommand) の JWT アクセストークン発行」は、login がリフレッシュトークン発行込みになるため文言整合が必要。アクセストークン＋リフレッシュトークン発行を表す文言へ整える（既存の `[x]` 状態は維持）。
- `AuthController#login`（`TODO.md:7`）は既に `[x]`。新規スタブの追加登録は発生しない（内側は既存メソッドのリネームのため）。
</content>
