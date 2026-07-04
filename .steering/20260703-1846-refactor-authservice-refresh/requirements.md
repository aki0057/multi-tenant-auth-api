# requirements

## 作業概要
`AuthService#refresh(RefreshCommand)`（`application/AuthService.java`、110〜146行目）は既存の完成済みメソッドだが、現在は `RefreshToken` に対する判定・生成をインラインで行っている。

1. 判定: `if (oldToken.revoked() || oldToken.expiresAt().isBefore(now))`（120行目）
2. ローテーション時の失効済みインスタンス生成: `refreshTokenRepository.save(new RefreshToken(oldToken.id(), oldToken.userId(), oldToken.tokenHash(), oldToken.expiresAt(), true));`（131〜132行目）

これらを、`RefreshToken` DomainObject 側に実装済みの判定メソッド・生成メソッド（別 steering `20260703-1845-feat-refreshtoken-domainobject` で実装）へ置き換える。

1. 判定 → `if (oldToken.isRevoked() || oldToken.isExpired(now))`
2. ローテーション時の失効済みインスタンス生成 → `refreshTokenRepository.save(oldToken.revoke());`

`AuthService#refresh` のメソッドシグネチャ（引数・戻り値・`@Transactional`）は変更しない。挙動（HTTP 401 変換・トランザクション境界・アクセストークン再発行・トークンローテーション）も不変。これは新規機能追加ではなく、`RefreshToken` 集約への責務移譲によるリファクタリングである。

## 前提条件（実施順序と着手可否）

本 steering は次を厳密な前提とする。**前提が満たされるまで、段階2（plan-verifier）の再検証・段階3（implementer）の実施へ進んではならない。**

1. 依存する 4 つの steering ディレクトリがすべて実装完了（tasklist の全チェック充足かつ `./mvnw test` で該当テストが green）していること。
   - `.steering/20260703-1842-feat-vo-refreshtokenid`（`RefreshTokenId` の検証実装）
   - `.steering/20260703-1843-feat-vo-tokenhash`（`TokenHash` の検証実装。および `AuthServiceTest` の付随修正）
   - `.steering/20260703-1844-feat-vo-rawrefreshtoken`（`RawRefreshToken` の検証実装）
   - `.steering/20260703-1845-feat-refreshtoken-domainobject`（`RefreshToken#isRevoked()` / `#isExpired(Instant)` / `#revoke()` の実装）
2. 特に `.steering/20260703-1845-feat-refreshtoken-domainobject` が未実施の間は、実コードの `RefreshToken.java` に `isRevoked()` / `isExpired(Instant)` / `revoke()` が存在せず、本 steering の変更内容（これらのメソッド呼び出しへの置き換え）はコンパイル不能である。したがって本 steering の着手は必ず 4 件すべての完了後でなければならない。
3. **前提充足の確認責任**: 4 件の前提が満たされているかどうかの確認は、本 steering 自身（requirements.md / tasklist.md）では担保できない（サブエージェントはコールド起動であり、他 steering の進捗を自律的に検知できないため）。したがって、5 件全体の実施順序（1842 → 1843 → 1844 → 1845 → 1846 の順で、各々の段階2〜5 を完了させてから次の steering の段階2 を dispatch する）を担保する責任は、パイプライン全体を駆動するメインセッション（オーケストレーター）が負う。

## 作業対象レイヤー
application（Service）

## 作業対象の種別
Service

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 (Jupiter) + Mockito（`@ExtendWith(MockitoExtension.class)`、`@Mock` / `@InjectMocks`）+ AssertJ。
- Spring MVC アノテーション: 使用しない（`AuthServiceTest` は Spring 未起動、プレーンな Mockito テスト）。

## 隣接レイヤー
- 1 つ外側のレイヤー: presentation（`AuthController#refresh`）
- 1 つ内側のレイヤー: domain（`RefreshToken` DomainObject の `isRevoked()` / `isExpired(Instant)` / `revoke()`。前提条件を参照——本 steering 着手時点では実装済みであること）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる（変更不可）。

- `presentation/AuthController.java`（`refresh` メソッド）が `authService.refresh(command)` を呼び出す。`AuthService#refresh` のシグネチャは変更しないため、`AuthController` 側の呼び出しコードにも影響はない。

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド（シグネチャは不変、内部実装のみ変更）。

- メソッド名: `refresh`
- 引数: `@NonNull RefreshCommand command`
- 戻り値: `RefreshResult`

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
該当なし（新規スタブは作成しない）。ただしこれは「内側レイヤーが元々不要」という意味ではなく、以下の条件付きの理由による。

- 前提条件（4 steering の完了）が満たされた後は、本 steering が呼び出す内側メソッド（`RefreshToken#isRevoked()` / `#isExpired(Instant)` / `#revoke()`）はすでに実在する。したがって本 steering の時点で新たにスタブとして作成すべき内側メソッドは存在しない（＝ tasklist に「内側レイヤーに作成したスタブのメソッド名を TODO.md へ追記する」項目を含めない）。
- 既存の内側メソッド呼び出し（`RefreshTokenRepository#findByTokenHash` / `#save`、`UserRepository#findById`、`RefreshTokenGenerator#generate`、`RefreshTokenHasher#hash`）はいずれも実装済みインターフェースで変更なし。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
プリミティブ型への依存を禁止する。本増分後も `AuthService#refresh` はプリミティブへの依存を増やさない。既存の `RefreshToken` / `User` / `RawRefreshToken` / `TokenHash` をそのまま利用し、新規に作成する DomainObject / ValueObject はない。

- 作成する DomainObject / ValueObject 名: なし（新規作成なし）

## 変更内容の詳細（補足）
`AuthService.java` の `refresh` メソッド本体のみを変更する（インポート・フィールド・シグネチャ・Javadoc の実質的な意味は変更しない。Javadoc 中の実装詳細に関する記述があれば整合するよう更新してよい）。

- 120行目: `if (oldToken.revoked() || oldToken.expiresAt().isBefore(now)) {` → `if (oldToken.isRevoked() || oldToken.isExpired(now)) {`
- 131〜132行目: `refreshTokenRepository.save(new RefreshToken(oldToken.id(), oldToken.userId(), oldToken.tokenHash(), oldToken.expiresAt(), true));` → `refreshTokenRepository.save(oldToken.revoke());`
- それ以外のロジック（トークン検索・ユーザー有効性確認・新トークン生成・保存・アクセストークン発行・例外変換）は変更しない。

## テスト仕様の詳細（補足）
既存 `AuthServiceTest.java` の `refresh` 系テスト（`refresh_success` / `refresh_tokenNotFound` / `refresh_tokenRevoked` / `refresh_tokenExpired` / `refresh_userNotFound` / `refresh_userInactive` / `refresh_tenantInactive`）は、外部から観測される挙動（戻り値・`BadCredentialsException` へのスロー変換・`refreshTokenRepository.save` の呼び出し回数と引数内容）を変更しないため、既存のアサーション内容自体は変更不要と想定される。ただし内部実装がモックの状態（`RefreshToken` インスタンスの生成方法）に依存していないかを実行して確認し、`green` を維持できない箇所があれば挙動を変えずに追随修正する（例: 新たに `isRevoked()` / `isExpired(Instant)` を呼び出す実装へ変わることで、`Mockito` のスタブ設定自体には影響しないはずだが、実行して確認すること）。
