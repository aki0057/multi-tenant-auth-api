# tasklist

チェックボックスは段階3（implementer）が上から順に埋める。`(条件: ...)` は該当時のみ実施。

## 作業対象の実装（presentation）

- [x] （TODO.md 登録: 非該当 — `AuthController#login` は `TODO.md:7` に既存の `[x]` のため新規登録しない）
- [x] `LoginResponse` を 3 引数 `(String accessToken, String refreshToken, String tokenType)` へ拡張する
- [x] `LoginResponse` の Javadoc（クラス説明・`@param`）を `RefreshResponse` と同形に追随させる（`refreshToken` の `@param` 追加）
- [x] `AuthController#login` を `LoginResult result = authService.login(new LoginCommand(...));` ＋ `new LoginResponse(result.accessToken(), result.refreshToken(), "Bearer")` へ切り替える
- [x] `AuthController#login` の Javadoc（返却物の説明）を、アクセストークン＋リフレッシュトークンを返す形へ追随させる

## 内側レイヤーの契約確定（application ／ 移行ブリッジ撤去）

- [x] `AuthService#login(LoginCommand): String`（`@Transactional(readOnly = true)` の旧シム、現 61-87 行）を削除する
- [x] `AuthService#loginWithRefreshToken(LoginCommand): LoginResult`（現 116-152 行）を `login` へリネームする（本体ロジックは変更しない）
- [x] リネーム後 `login` の Javadoc から「2 パス移行のための一時名メソッド…」段落を撤去し、通常の login 説明へ整える
- [x] 旧 `login(LoginCommand): String` の残存参照が production に無いことを実装後に再確認する（grep 済み: `AuthController#login` のみ）

## テスト（正常系・異常系の追随）

- [x] 正常系: `AuthControllerTest#login_success` の `authService.login(any())` スタブを `LoginResult` 返却へ変更し、レスポンスが `accessToken` / `refreshToken` / `tokenType` の 3 フィールド JSON になることを検証する（`$.refreshToken` の jsonPath 追加）
- [x] 異常系: `AuthControllerTest` の `login_invalidCredentials` / `login_unexpectedException` / `login_invalidRequest` が新シグネチャで整合していることを確認・追随する
- [x] 正常系: `LoginIntegrationTest` の 200 OK テストに、レスポンス JSON が `accessToken` / `refreshToken` / `tokenType` を含む検証を追加する
- [x] 異常系: `LoginIntegrationTest` の各異常系が新レスポンス形で整合していることを確認・追随する
- [x] `AuthServiceTest`（application）: 旧 `String login(LoginCommand)` を対象とする `login_*` テスト群（現 89-163 行）を、削除される旧シムに合わせて撤去する
- [x] `AuthServiceTest`（application）: `loginWithRefreshToken_*` テスト群（現 176-289 行）を、リネーム後の `authService.login(...)` 呼び出しへ追随させる（メソッド名参照・`DisplayName`／一時名セクションコメントの整備を含む）

## TODO.md 整合

- [x] （内側スタブ登録: 非該当 — 内側は新規スタブではなく既存メソッドのリネームのため TODO.md への新規登録は行わない）
- [x] （DomainObject/VO 登録: 非該当 — 作成しない）
- [x] `TODO.md:13`「AuthService#login(LoginCommand) の JWT アクセストークン発行」の文言を、login がアクセストークン＋リフレッシュトークン発行込みになる最終形に合わせて整える（既存の `[x]` 状態は維持する）
- [x] 今回の作業対象 `AuthController#login`（`TODO.md:7`）は既に `[x]` で完了済みであることを確認する（状態変更なし）
</content>
