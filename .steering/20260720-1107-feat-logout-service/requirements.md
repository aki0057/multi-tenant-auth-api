# requirements

## 作業概要
ログアウトユースケースの application(Service) 層本体を実装する。現在スタブ（`UnsupportedOperationException`）である `AuthService.logout(LogoutCommand)` を、失効方式で実装する。提示された生リフレッシュトークンをハッシュ化して該当レコードを検索し、見つかった場合は無条件に失効（`revoke`）して保存する。物理削除は行わない。

契約は「冪等・例外を投げない」。以下のいずれの場合も例外を出さず正常終了（`void` 復帰）する。
- `command.refreshToken()` が `null`
- `RawRefreshToken` の VO 検証に失敗する（`IllegalArgumentException`：`null`・空白のみ等の形式不正）
- DB に該当トークンが存在しない（`findByTokenHash` が空）

該当トークンが見つかった場合は、失効済み・期限切れであるかを問わず無条件に `revoke()` → `save()` する。所有ユーザー・テナントの有効性チェックは行わない。失効対象は tokenHash で特定した 1 件のみ（全デバイスログアウトはスコープ外）。

## 作業対象レイヤー
application(Service)

## 作業対象の種別
Service

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 + Mockito（既存 `AuthServiceTest` の単体テストパターンに合わせる。依存はモック化）
- Spring MVC アノテーション: 使用しない（Service 層のため）。本体には `@Transactional` を付与する（既存スタブと同じ）。

## 隣接レイヤー
- 1 つ外側のレイヤー: presentation(`AuthController.logout`) — 実装済み。`authService.logout(new LogoutCommand(refreshToken))` を呼ぶ。
- 1 つ内側のレイヤー: domain（`RawRefreshToken` / `TokenHash` / `RefreshTokenHasher.hash` / `RefreshTokenRepository.findByTokenHash` / `RefreshTokenRepository.save` / `RefreshToken.revoke`） — いずれも既存・実装済み。

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
外側（`AuthController.logout`）は既に存在する。以下のシグネチャに作業対象を合わせる（変更しない）。
- メソッド名: `AuthService.logout`
- 引数: `LogoutCommand command`（`@NonNull`）。`LogoutCommand` は `record LogoutCommand(String refreshToken)`（作成済み・完成扱い）。`refreshToken` は未提示時 `null`。
- 戻り値: `void`

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド。
- メソッド名: `logout`
- 引数: `@NonNull LogoutCommand command`
- 戻り値: `void`（`@Transactional` 付与）

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
内側の部品はすべて既存であり、新規スタブの作成は不要（新規 DomainObject/ValueObject も不要）。以下の既存 API を利用する。
- `new RawRefreshToken(String value)` — VO。`null`/空白で `IllegalArgumentException`。
- `RefreshTokenHasher.hash(RawRefreshToken)` → `TokenHash`
- `RefreshTokenRepository.findByTokenHash(TokenHash)` → `Optional<RefreshToken>`
- `RefreshToken.revoke()` → 失効済みの新インスタンス（`RefreshToken`、immutable record）
- `RefreshTokenRepository.save(RefreshToken)` → `RefreshToken`

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
Command が保持するプリミティブ（`String refreshToken`）は、Service の入口で ValueObject（`RawRefreshToken`）へ変換してから内側へ渡す。プリミティブのままドメイン層へ持ち込まない。VO 変換時の `IllegalArgumentException` は握りつぶし（冪等契約のため）、`void` で正常終了する。
- 作成する DomainObject / ValueObject 名: なし（すべて既存のものを利用。新規作成しない）
