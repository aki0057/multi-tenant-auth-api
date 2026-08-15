# requirements

## 2パス方式の位置づけ（本 steering は Pass 1）
本作業「login エンドポイントのレスポンスに refreshToken を含める」は、presentation（`LoginResponse` / `AuthController`）と
application（`LoginResult` / `AuthService`）の両レイヤーにまたがる。単一レイヤー/単一種別の原則を守りつつ、
各パス末尾で必ずプロジェクト全体が green（tester の green ゲートを通過）になるよう、**2パス分割＋一時互換ブリッジ**で進める。

### 互換ブリッジの全体像（確定）
- **Pass 1（本 steering, application 単独）**:
  - application 層に出力 DTO `LoginResult` を新設する。
  - `AuthService` に本実装を**一時名の新メソッド** `loginWithRefreshToken(LoginCommand) -> LoginResult` として追加する。
  - 既存 `String login(LoginCommand)` は**シムとして温存**し一切変更しない。`AuthController` / `LoginResponse` も無変更。
  - これにより Pass 1 末尾でプロジェクト全体が green のまま（既存の login 経路は不変、新メソッドは追加のみ）。
- **Pass 2（別途依頼, presentation ＋ ブリッジ撤去）**:
  - `LoginResponse` を3引数 `(accessToken, refreshToken, tokenType)` 化。
  - `AuthController.login` を新メソッド呼び出しへ切替。
  - 旧シム `String login(LoginCommand)` を削除し、一時名メソッドを `login` にリネーム。
  - 最終状態は要件どおり `AuthService.login(LoginCommand) -> LoginResult`。一時名は消滅する。

**本 steering は Pass 1 に限定する。** Pass 2 の作業（presentation 変更・シム撤去・リネーム）は本 steering の対象外である。

### 単一レイヤー原則の遵守（Pass 1 単体）
Pass 1 の作業対象は **application(Service) の単一レイヤー・単一種別（Service）のみ**である。
presentation は一切触れない。`LoginResult` は application 層の出力 DTO であり、Service 作業の一部として同一レイヤー内で完結する。
新規の内側スタブ・DomainObject・ValueObject・DI 配線も発生しない（すべて refresh 実装時に作成済み）。
したがって Pass 1 単体では単一レイヤー/単一種別の原則を厳守している。

## 作業概要
`AuthService` に、認証成功後アクセストークンとリフレッシュトークンの双方を発行して返す本実装メソッドを、
一時名 `loginWithRefreshToken(LoginCommand) -> LoginResult` として追加する。リフレッシュトークンは
「新規発行のみ」（ローテーションではない）で、生成→ハッシュ化→永続化を行う。あわせて application 層の
出力 DTO `LoginResult`（`record LoginResult(String accessToken, String refreshToken)`）を新設する。
既存 `String login(LoginCommand)` はシムとして温存し変更しない。

## 作業対象レイヤー
application(Service)

## 作業対象の種別
Service（application）

## 使用するテスト・フレームワーク等
- テストフレームワーク: Mockito（依存をモック化して `AuthService` を単体テスト。`AuthServiceTest` に追記）
- Spring MVC アノテーション: 使用しない（Service 層のため）

## 隣接レイヤー
- 1 つ外側のレイヤー: presentation（`AuthController.login`）。※ Pass 1 では変更しない。Pass 2 で新メソッドへ切替。
- 1 つ内側のレイヤー: domain（`UserRepository` / `RefreshTokenRepository`、`AccessTokenProvider` / `RefreshTokenGenerator` / `RefreshTokenHasher` / `RefreshTokenExpirationPolicy`、`User` / `RefreshToken` / 各 VO、`java.time.Clock`）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
Pass 1 の時点では、外側 `AuthController` は既存 `String login(LoginCommand)` シムを呼び続けており、
一時名の新メソッドを呼び出す外側は**まだ存在しない**（Pass 2 で切替）。
したがって Pass 1 の新メソッドは、Pass 2 で `AuthController.login` が満たすべき最終契約に前もって合わせておく。
- 最終的に外側が呼び出す形（Pass 2）: `LoginResult result = authService.login(new LoginCommand(...))`
- Pass 1 で追加する一時名メソッドの契約: 引数 `LoginCommand`、戻り値 `LoginResult`
  （Pass 2 でこのメソッドを `login` にリネームすれば、上記の最終契約に一致する）

## 作業対象メソッドのシグネチャ
Pass 1 で追加する単一メソッド。
- メソッド名: `loginWithRefreshToken`（一時名。Pass 2 で `login` へリネームし旧シムを撤去する）
- 引数: `@NonNull LoginCommand command`
- 戻り値: `LoginResult`
- トランザクション: `@Transactional`（readOnly ではない。リフレッシュトークンの永続化で書き込みが発生するため）
- 挙動:
  - 認証部分は既存 `login` と同じ。`command` のプリミティブを Service 入口で VO（`TenantCode` / `Email` / `RawPassword`）へ変換し、
    変換失敗（`IllegalArgumentException`）・ユーザー不在・パスワード不一致（`AuthenticationFailedException`）を
    一律 `BadCredentialsException("Invalid credentials")`（HTTP 401 相当）へ変換する。
  - 認証成功後、アクセストークンを `accessTokenProvider.issue(user)` で発行。
  - リフレッシュトークンを**新規発行のみ**（ローテーションしない＝旧トークンの検索・失効は行わない）:
    `now = clock.instant()`、`rawRefreshToken = refreshTokenGenerator.generate()`、`hash = refreshTokenHasher.hash(rawRefreshToken)`、
    `refreshTokenRepository.save(new RefreshToken(null, user.tenantId(), user.userId(), hash, now.plus(refreshTokenExpirationPolicy.expiration()), false))`。
  - 戻り値: `new LoginResult(accessToken, rawRefreshToken.value())`。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
本作業で**新規に作成する内側スタブは無い**。必要な内側依存は refresh 実装時にすべて作成・実装済みで、`AuthService` に DI 済みである。
したがって TODO.md へ新規登録する内側スタブは無い。利用する既存の内側契約（いずれも実装済み・変更しない）:
- `AccessTokenProvider.issue(User) -> String`
- `RefreshTokenGenerator.generate() -> RawRefreshToken`
- `RefreshTokenHasher.hash(RawRefreshToken) -> TokenHash`
- `RefreshTokenExpirationPolicy.expiration() -> Duration`
- `RefreshTokenRepository.save(RefreshToken)`
- `UserRepository.findByTenantCodeAndEmail(TenantCode, Email) -> Optional<User>`
- `java.time.Clock.instant()`
- ドメイン: `new RefreshToken(null, tenantId, userId, hash, expiresAt, false)`、`user.authenticate(rawPassword, passwordVerifier)`

補足: `LoginResult` は内側スタブではなく application 層の出力 DTO（ロジックを持たない record、`RefreshResult` と同型）である。
Command / `RefreshResult` と同様に実処理を持たないため作成と同時に完成扱いとし、`TODO.md` には登録しない。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
プリミティブ型を Service 本体・ドメイン層へ持ち込まない。`LoginCommand` のプリミティブは Service 入口で VO へ変換する。
本作業で**新規作成する DomainObject / ValueObject は無い**。以下の既存モデルを利用する（すべて実装済み）。
- ValueObject: `TenantCode` / `Email` / `RawPassword` / `RawRefreshToken` / `TokenHash`
- DomainObject: `User` / `RefreshToken`
- 作成する DomainObject / ValueObject 名: なし（既存を利用）

## TODO.md の扱い（判断を明記）
Pass 1 で追加する一時名メソッド `loginWithRefreshToken` は、Pass 2 で `login` に統合・リネームされ**消滅する**中間生成物である。
恒久的な作業対象ではないため、**Pass 1 では TODO.md に登録しない**。
- 理由: TODO.md は各レイヤーのスタブ／恒久的な実装対象を記録する台帳であり、次パスで消える一時名を積むと不整合の原因になる。
- 既存台帳の状態: `Service (application)` 章の `AuthService#login(LoginCommand) の JWT アクセストークン発行` は既に `[x]`。本 refreshToken 発行は
  最終的にこの `login` に統合されるため、恒久的な TODO 項目としては Pass 2 完了時点で `login` として一貫する。
- 結論: Pass 1 では TODO.md を変更しない。TODO.md の整合は Pass 2（シム撤去・リネームで最終形 `AuthService.login -> LoginResult` に収束する時点）で行う。
