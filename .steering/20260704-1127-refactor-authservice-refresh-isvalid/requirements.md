# requirements

## 作業概要
`AuthService#refresh(RefreshCommand)`（`application/AuthService.java`、110〜145行目、既存の完成済みメソッド）は現在、リフレッシュトークンの有効性判定をインラインで行っている。

```java
Instant now = clock.instant();
if (oldToken.isRevoked() || oldToken.isExpired(now)) {
    throw new InvalidRefreshTokenException();
}
```

これを、`RefreshToken` DomainObject 側に実装済みの単一判定メソッド `isValid(Instant)`（別 steering `20260704-1125-feat-refreshtoken-isvalid` で実装）へ委譲する形へ置き換える。

```java
Instant now = clock.instant();
if (!oldToken.isValid(now)) {
    throw new InvalidRefreshTokenException();
}
```

`now` の取得（`clock.instant()`）は、ローテーション時に発行する新トークンの `expiresAt` 計算（`now.plus(REFRESH_TOKEN_EXPIRATION)`）にも使われているため、application 層（`refresh` メソッド内のローカル変数）にそのまま残す。例外送出（`InvalidRefreshTokenException` のスロー、および呼び出し元 catch 節での `BadCredentialsException` への変換）の責務も `AuthService` に残す。

`AuthService#refresh` のメソッドシグネチャ（引数・戻り値・`@Transactional`）は変更しない。挙動（HTTP 401 変換・トランザクション境界・アクセストークン再発行・トークンローテーション）も不変。これは新規機能追加ではなく、「トークンが使用可能か」という判定条件の定義をドメイン（`RefreshToken`）へ一元化するリファクタリングである。

参考: 同型の前例として `.steering/20260703-1846-refactor-authservice-refresh`（`isRevoked()`/`isExpired()`/`revoke()` への委譲）、および `.steering/20260704-1049-refactor-user-isactive`（`User#isActive()` への一元化）がある。

## 前提条件（実施順序と着手可否）

本 steering は次を厳密な前提とする。**前提が満たされるまで、段階2（plan-verifier）の再検証・段階3（implementer）の実施へ進んではならない。**

1. 依存する steering ディレクトリ `.steering/20260704-1125-feat-refreshtoken-isvalid`（`RefreshToken#isValid(Instant)` の実装）が実装完了（tasklist の全チェック充足かつ `./mvnw test` で該当テストが green）していること。
2. 本 steering 作成時点（2026-07-04 11:27）で、実コードの `RefreshToken.java` には `isValid(Instant)` がまだ存在しない（`isExpired(Instant)` / `isRevoked()` / `revoke()` のみ実装済み）。したがって前提が満たされるまでの間、本 steering の変更内容（`isValid(Instant)` 呼び出しへの置き換え）はコンパイル不能である。
3. **前提充足の確認責任**: 前提が満たされているかどうかの確認は、本 steering 自身（requirements.md / tasklist.md）では担保できない（サブエージェントはコールド起動であり、他 steering の進捗を自律的に検知できないため）。したがって、2件の steering の実施順序（`20260704-1125-feat-refreshtoken-isvalid` の段階2〜5 を完了させてから、本 steering の段階2 を dispatch する）を担保する責任は、パイプライン全体を駆動するメインセッション（オーケストレーター）が負う。

## 作業対象レイヤー
application（Service）

## 作業対象の種別
Service

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 (Jupiter) + Mockito（`@ExtendWith(MockitoExtension.class)`、`@Mock` / `@InjectMocks`）+ AssertJ。
- Spring MVC アノテーション: 使用しない（`AuthServiceTest` は Spring 未起動、プレーンな Mockito テスト）。

## 隣接レイヤー
- 1 つ外側のレイヤー: presentation（`AuthController#refresh`）
- 1 つ内側のレイヤー: domain（`RefreshToken#isValid(Instant)`。前提条件を参照——本 steering 着手時点では実装済みであること）

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

- 前提条件（`20260704-1125-feat-refreshtoken-isvalid` の完了）が満たされた後は、本 steering が呼び出す内側メソッド `RefreshToken#isValid(Instant)` はすでに実在する。したがって本 steering の時点で新たにスタブとして作成すべき内側メソッドは存在しない（＝ tasklist に「内側レイヤーに作成したスタブのメソッド名を TODO.md へ追記する」項目を含めない）。
- 既存の内側メソッド呼び出し（`RefreshTokenRepository#findByTokenHash` / `#save`、`UserRepository#findById`、`RefreshTokenGenerator#generate`、`RefreshTokenHasher#hash`、`RefreshToken#revoke()`）はいずれも実装済みで変更なし。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
プリミティブ型への依存を禁止する。本増分後も `AuthService#refresh` はプリミティブへの依存を増やさない。既存の `RefreshToken` / `User` / `RawRefreshToken` / `TokenHash` をそのまま利用し、新規に作成する DomainObject / ValueObject はない。

- 作成する DomainObject / ValueObject 名: なし（新規作成なし）

## 変更内容の詳細（補足）
`AuthService.java` の `refresh` メソッド本体のみを変更する（インポート・フィールド・シグネチャ・Javadoc の実質的な意味は変更しない。Javadoc 中の実装詳細に関する記述があれば整合するよう更新してよい）。

- 120〜122行目: `if (oldToken.isRevoked() || oldToken.isExpired(now)) { throw new InvalidRefreshTokenException(); }` → `if (!oldToken.isValid(now)) { throw new InvalidRefreshTokenException(); }`
- `Instant now = clock.instant();`（119行目）は削除せず application 層に残す（新トークンの `expiresAt` 計算 `now.plus(REFRESH_TOKEN_EXPIRATION)` で再利用するため）。
- それ以外のロジック（トークン検索・ユーザー有効性確認・新トークン生成・保存・アクセストークン発行・例外変換）は変更しない。

## テスト仕様の詳細（補足）
既存 `AuthServiceTest.java` の `refresh` 系テスト（`refresh_success` / `refresh_tokenNotFound` / `refresh_tokenRevoked` / `refresh_tokenExpired` / `refresh_userNotFound` / `refresh_userInactive` など）は、外部から観測される挙動（戻り値・`BadCredentialsException` へのスロー変換・`refreshTokenRepository.save` の呼び出し回数と引数内容）を変更しないため、既存のアサーション内容自体は変更不要と想定される。ただし内部実装がモックの状態（`RefreshToken` インスタンスの生成方法や `isValid` の内部呼び出し）に依存していないかを実行して確認し、`green` を維持できない箇所があれば挙動を変えずに追随修正する。

## 変更対象ファイル一覧（本タスクが操作するすべてのファイル）

| 操作 | ファイルパス（`src/` 以下） | 内容 |
|----|-----------------|----|
| 変更 | `main/.../application/AuthService.java` | `refresh` メソッド内の判定 `oldToken.isRevoked() \|\| oldToken.isExpired(now)` を `!oldToken.isValid(now)` へ置き換え |
| 変更（確認のみ、想定は無変更） | `test/.../application/AuthServiceTest.java` | 既存 `refresh` 系テストを実行し green を確認。挙動差異があれば挙動を変えずに追随修正 |
