# requirements

## 作業概要

`AuthService#refresh(RefreshCommand command)` を実装する。現状はスタブ（`UnsupportedOperationException` をスロー）であり、これを以下のユースケースへ置き換える。

1. `RefreshCommand#refreshToken()`（生のリフレッシュトークン文字列）を `RawRefreshToken` VO に変換する。
2. `RefreshTokenHasher#hash` でハッシュ化し、`RefreshTokenRepository#findByTokenHash` で該当レコードを検索する（不存在なら無効トークン扱い）。
3. 取得した `RefreshToken` の有効性検証（期限切れ・失効済みかどうか）を行う。無効なら無効トークン扱い。
4. `RefreshToken#userId()` を用いて `UserRepository#findById` でユーザーを取得し（不存在なら無効トークン扱い）、`User#userIdIsActive()` / `User#tenantIdIsActive()` を確認する（いずれか false なら無効トークン扱い）。
5. 上記いずれかで無効と判定された場合は `InvalidRefreshTokenException`（新規 domain 例外）をスローし、`AuthService` 内でキャッチして `BadCredentialsException`（401 相当）に変換する。原因（不存在・期限切れ・失効・ユーザー無効・テナント無効のいずれか）は外部に一切露出させない。
6. 検証成功時は「ローテーション」を行う。旧トークンを失効状態にした新しい `RefreshToken` インスタンスを組み立てて `RefreshTokenRepository#save` で保存し、`RefreshTokenGenerator#generate` で新しい生トークンを生成、`RefreshTokenHasher#hash` でハッシュ化した新しい `RefreshToken` インスタンスを組み立てて `RefreshTokenRepository#save` で保存する（旧トークン失効＋新トークン保存を同一トランザクションで原子的に行う）。
7. `AccessTokenProvider#issue` でアクセストークンを再発行し、新アクセストークンと新しい生リフレッシュトークンを `RefreshResult` として返す。

有効期限の判定はテスト容易性のため `java.time.Clock`（DI 対象、`config` にシンプルな `@Bean` を追加）を用いて現在時刻を取得して行う。新トークンの有効期間は `AuthService` 内の定数（例: 14日）として保持する（設定ファイル外出しは本作業のスコープ外）。

## 単一レイヤー原則との整合注記

本増分の主目的は application(Service) 層の `AuthService#refresh` 実装である。ただし内側の domain 層（`RefreshToken` / VO 3種 / `RefreshTokenRepository` / `RefreshTokenGenerator` / `RefreshTokenHasher` / `InvalidRefreshTokenException` / `UserRepository#findById`）はまだ存在しないため、`docs/file-change-workflow.md` の「スタブの定義」に従いすべてスタブとして新規作成する。

さらに、次の2点は `docs/file-change-workflow.md` の「スタブの定義」の範囲内であり、かつユーザーが事前承認済みの方針（作業内容の前提コンテキストを参照）である。plan-verifier はこれらを単一レイヤー違反として差し戻さないこと。

- **infrastructure 層への一時スタブアダプタ追加**: `RefreshTokenGenerator` / `RefreshTokenHasher` / `RefreshTokenRepository` は `@SpringBootTest` を用いる既存の結合テスト（`AuthControllerTest` 等）が DI コンテナで具象 Bean を解決できるよう、`@Component`（または `@Repository`）付きの一時スタブアダプタを同時に作成する。メソッド本体は `UnsupportedOperationException` をスローするのみとする（本物のトークン生成・ハッシュ化・永続化ロジックは実装しない）。既存の `StubAccessTokenProvider` と同じ扱い。
- **既存 `UserRepository` インターフェースへの `findById` 追加に伴う既存実装クラスの追随**: `UserRepository`（インターフェース）に `findById` を追加すると、既存の具象クラス `UserRepositoryImpl`（既に実装済み・スタブではない）がコンパイルエラーになるため、`UserRepositoryImpl` にも `findById` のスタブメソッド（`UnsupportedOperationException` をスロー、`// TODO` 目印付き）を追加する。これは新規スタブの追加に伴う必然的な追随修正であり、単一レイヤー違反ではない。

また、`java.time.Clock` を `AuthService` へ DI するために `config` パッケージへ `Clock` の `@Bean` を追加する。これは DDD レイヤー（API/Service/DomainObject/VO/Repository/infrastructure.mapper/infrastructure.security）のいずれにも該当しない Spring 配線であり、既存の `SecurityConfig` への `/refresh` permitAll 追加と同様の「必要最小限の付随的変更」として扱う。

## 作業対象レイヤー

application(Service)

## 作業対象の種別

Service

## 使用するテスト・フレームワーク等

- テストフレームワーク: JUnit5 + Mockito（`@ExtendWith(MockitoExtension.class)`、`@Mock`、`@InjectMocks`）。既存 `AuthServiceTest` と同一クラスに `refresh` 用のテストを追加する。
- Spring MVC アノテーション: なし（`AuthServiceTest` は Spring コンテキストを使わない純粋な Mockito テスト）。`AuthService` 本体では既存の `@Service` / `@Transactional` を利用する（`refresh` には書き込み用の `@Transactional`（`readOnly` 指定なし）を付与する）。

## 隣接レイヤー

- 1 つ外側のレイヤー: presentation（`AuthController#refresh`。既に実装済み・シグネチャ変更禁止）
- 1 つ内側のレイヤー: domain（`RefreshToken` / VO / `RefreshTokenRepository` / `RefreshTokenGenerator` / `RefreshTokenHasher` / `InvalidRefreshTokenException`。すべて新規スタブ）。加えて既存の domain（`User` / `UserRepository` / `AccessTokenProvider`）を利用する。

## 外側レイヤーとの契約

外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる（変更禁止）。

`AuthController#refresh` が以下のように呼び出している。

```java
RefreshResult result = authService.refresh(new RefreshCommand(request.getRefreshToken()));
```

- メソッド名: `refresh`
- 引数: `RefreshCommand command`（`record RefreshCommand(String refreshToken)`、作成済み・完成扱い）
- 戻り値: `RefreshResult`（`record RefreshResult(String accessToken, String refreshToken)`、作成済み・完成扱い）

## 作業対象メソッドのシグネチャ

作業概要を達成するための単一メソッド。

- メソッド名: `refresh`
- 引数: `RefreshCommand command`
- 戻り値: `RefreshResult`

```java
@Transactional
public RefreshResult refresh(@NonNull RefreshCommand command)
```

## 内側レイヤーへの契約

内側はまだ存在しないため、以下をすべてスタブとして新規作成する（実装しない・`// TODO` のみ）。

### `RefreshTokenRepository`（domain.repository、新規インターフェース）

- 配置: `src/main/java/io/github/aki0057/multitenant/auth/domain/repository/RefreshTokenRepository.java`
- メソッド1: `Optional<RefreshToken> findByTokenHash(TokenHash tokenHash)` — ハッシュ値で該当トークンを検索する
- メソッド2: `RefreshToken save(RefreshToken refreshToken)` — 新規トークンの永続化・既存トークンの失効状態更新の両方に使う
- スタブ方針: interface 宣言のみ（メソッド本体なし）

### `RefreshTokenGenerator`（domain.service、新規ポート）

- 配置: `src/main/java/io/github/aki0057/multitenant/auth/domain/service/RefreshTokenGenerator.java`
- メソッド: `RawRefreshToken generate()` — セキュアランダムな生リフレッシュトークンを生成する
- スタブ方針: interface 宣言のみ（メソッド本体なし）

### `RefreshTokenHasher`（domain.service、新規ポート）

- 配置: `src/main/java/io/github/aki0057/multitenant/auth/domain/service/RefreshTokenHasher.java`
- メソッド: `TokenHash hash(RawRefreshToken rawRefreshToken)` — 生トークンを SHA-256 でハッシュ化する
- スタブ方針: interface 宣言のみ（メソッド本体なし）

### `RefreshToken`（domain.model、新規 DomainObject。詳細は「ドメインモデルの利用」参照）

- メソッド: なし（本増分では純粋なデータ保持のみ。期限切れ・失効判定と `revoke()` の実装は後続の domain 増分で追加する。判定ロジックは本増分では `AuthService` 内に一時的に実装する）
- 引数（record コンポーネント）: `RefreshTokenId id, UserId userId, TokenHash tokenHash, Instant expiresAt, boolean revoked`
- 戻り値: なし（データ保持のみ）

### `InvalidRefreshTokenException`（domain.exception、新規ドメイン例外）

- 配置: `src/main/java/io/github/aki0057/multitenant/auth/domain/exception/InvalidRefreshTokenException.java`
- 引数: なし（`AuthenticationFailedException` と同様、メッセージ固定の no-arg コンストラクタ）
- 戻り値: 該当なし（`RuntimeException` を継承）
- 補足: `Spring` に依存しないドメイン例外として完全実装する（スタブではない。`AuthenticationFailedException` と同じ扱いで、`TODO.md` には登録しない）。`AuthService#refresh` 内で発生させ、同メソッド内で捕捉して `BadCredentialsException` に変換する。

### `UserRepository#findById`（既存インターフェースへのメソッド追加）

- メソッド名: `findById`
- 引数: `UserId userId`
- 戻り値: `Optional<User>`
- 補足: 既存の `UserRepositoryImpl` にもスタブメソッド（`UnsupportedOperationException`、`// TODO` 目印）を追加してコンパイルを通す（「単一レイヤー原則との整合注記」参照）。

### infrastructure 層への一時スタブアダプタ（`@SpringBootTest` のコンテキスト起動用。「単一レイヤー原則との整合注記」参照）

- `StubRefreshTokenGenerator`（`infrastructure.security`、`@Component`、`RefreshTokenGenerator` を実装、`generate()` は `UnsupportedOperationException` をスロー）
- `StubRefreshTokenHasher`（`infrastructure.security`、`@Component`、`RefreshTokenHasher` を実装、`hash(RawRefreshToken)` は `UnsupportedOperationException` をスロー）
- `StubRefreshTokenRepository`（`infrastructure.persistence.repository`、`@Repository`、`RefreshTokenRepository` を実装、`findByTokenHash` / `save` はいずれも `UnsupportedOperationException` をスロー）

### `config.ClockConfig`（新規、Spring 配線。DDD レイヤー種別には該当しない付随的変更）

- `@Configuration` クラスに `@Bean public Clock clock() { return Clock.systemUTC(); }` を追加する

## ドメインモデルの利用

プリミティブ型への依存を禁止する。必ず DomainObject または ValueObject を作成または既存のものを利用する。ただし `RefreshCommand`（Command）はプリミティブ型（`String refreshToken`）を保持してよく、`AuthService#refresh` の入口で `RawRefreshToken` VO へ変換してから使用する（プリミティブのまま後段のロジックへ渡さない）。

作成する DomainObject / ValueObject はすべてスタブとする（実装しない・`// TODO` のみ）。

- 作成する ValueObject 名:
  - `RefreshTokenId(Long value)` — リフレッシュトークンの主キーを表す VO（`domain/model/vo/RefreshTokenId.java`）。値検証ロジックは未実装（`// TODO`）。
  - `TokenHash(String value)` — リフレッシュトークンの SHA-256 ハッシュ値（hex文字列）を表す VO（`domain/model/vo/TokenHash.java`）。値検証ロジックは未実装（`// TODO`）。
  - `RawRefreshToken(String value)` — クライアントへ渡す生のリフレッシュトークン文字列を表す VO（`domain/model/vo/RawRefreshToken.java`）。値検証ロジックは未実装（`// TODO`）。
- 作成する DomainObject 名:
  - `RefreshToken(RefreshTokenId id, UserId userId, TokenHash tokenHash, Instant expiresAt, boolean revoked)` — リフレッシュトークン集約（`domain/model/RefreshToken.java`）。本増分では純粋なデータ record のみ（期限切れ・失効判定・`revoke()` は後続 domain 増分で実装、`// TODO` を付す）。

## 実装メモ（`AuthService#refresh` 内のロジック、参考実装）

```java
private static final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(14);

@Transactional
public RefreshResult refresh(@NonNull RefreshCommand command) {
    try {
        RawRefreshToken rawRefreshToken = new RawRefreshToken(command.refreshToken());
        TokenHash tokenHash = refreshTokenHasher.hash(rawRefreshToken);

        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        Instant now = clock.instant();
        if (oldToken.revoked() || oldToken.expiresAt().isBefore(now)) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(oldToken.userId())
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!user.userIdIsActive() || !user.tenantIdIsActive()) {
            throw new InvalidRefreshTokenException();
        }

        refreshTokenRepository.save(new RefreshToken(
                oldToken.id(), oldToken.userId(), oldToken.tokenHash(), oldToken.expiresAt(), true));

        RawRefreshToken newRawToken = refreshTokenGenerator.generate();
        TokenHash newTokenHash = refreshTokenHasher.hash(newRawToken);
        refreshTokenRepository.save(new RefreshToken(
                null, user.userId(), newTokenHash, now.plus(REFRESH_TOKEN_EXPIRATION), false));

        String accessToken = accessTokenProvider.issue(user);
        return new RefreshResult(accessToken, newRawToken.value());
    } catch (InvalidRefreshTokenException e) {
        throw new BadCredentialsException("Invalid refresh token");
    }
}
```

この実装メモは implementer の参考であり、Javadoc・命名は implementer の裁量で調整してよい（挙動・呼び出し順序は変更しないこと）。

## テストケース方針（参考）

- 正常系: トークン検索成功・未失効・未期限切れ・ユーザー有効・テナント有効 → `RefreshResult` に新アクセストークンと新生トークン文字列が入ること、旧トークンが `revoked=true` で保存されること、新トークンが `revoked=false` で保存されること。
- 異常系（いずれも `BadCredentialsException` かつ `accessTokenProvider.issue` が呼ばれないこと）:
  - トークンが見つからない（`findByTokenHash` が空）
  - トークンが失効済み（`revoked=true`）
  - トークンが期限切れ（`expiresAt` が現在時刻より過去）
  - ユーザーが見つからない（`findById` が空）
  - ユーザーが無効（`userIdIsActive=false`）
  - テナントが無効（`tenantIdIsActive=false`）
