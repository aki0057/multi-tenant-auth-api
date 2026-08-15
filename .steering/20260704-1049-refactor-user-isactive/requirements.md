# requirements

## 作業概要

User ドメインモデル（`domain/model/User.java`、record）に、公開メソッド `isActive()`（`userIdIsActive && tenantIdIsActive` を返す）を追加し、「有効なユーザー＝ユーザー自身が有効かつテナントも有効」というドメイン知識の定義を User に一元化するリファクタリング。

具体的には次の変更を行う。

1. `User` に `public boolean isActive()` を追加する（`userIdIsActive && tenantIdIsActive` を返す）。
2. `User#authenticate(RawPassword, PasswordVerifier)` 内の先頭 2 つの if（`!userIdIsActive` チェックと `!tenantIdIsActive` チェック）を `if (!isActive())` の 1 つに置き換える（例外は従来どおり `AuthenticationFailedException`）。
3. `application/AuthService.java` の `refresh(RefreshCommand)` 内の `if (!user.userIdIsActive() || !user.tenantIdIsActive())` を `if (!user.isActive())` に置き換える（例外は従来どおり `InvalidRefreshTokenException`）。

record コンポーネント（`userIdIsActive` / `tenantIdIsActive`）の変更・新規スタブの作成は行わない。外部から見た振る舞い（スローされる例外・戻り値）は変更しない。

**注意**: 本ワークフローは本来「外側→内側へ単一レイヤーを新規構築する」前提だが、今回は既存実装の責務集約であり、domain（DomainObject）と application（Service の呼び出し箇所変更）の 2 レイヤーにまたがる。中心となる作業対象を `User#isActive` の追加と定め、その軸で以下を整理する（前例: `.steering/20260628-1031-refactor-user-authenticate/`）。

---

## 作業対象レイヤー

domain（DomainObject）を主軸とし、付随して application（`AuthService#refresh` の呼び出し箇所変更）を含む。

## 作業対象の種別

DomainObject（主軸）

---

## 使用するテスト・フレームワーク等

- テストフレームワーク: JUnit 5（`UserTest` は Spring コンテキスト不要のためプレーンな JUnit のみ、モックなし）、Mockito（`AuthServiceTest` は既存どおり `@ExtendWith(MockitoExtension.class)` を使用。振る舞い不変のため既存の `refresh` 系テストは変更なしで green を維持する想定）
- Spring MVC アノテーション: なし（domain / application 層のみで presentation 層の変更なし）

---

## 隣接レイヤー

- 1 つ外側のレイヤー: application（`AuthService`）
- 1 つ内側のレイヤー: なし（`isActive()` は同一 record 内のプリミティブ boolean コンポーネントの論理積を返すのみで、内側レイヤーへの新規呼び出しは発生しない）

---

## 外側レイヤーとの契約（条件: 作業対象が API でない場合）

外側（`AuthService`）は既存メソッドであり、シグネチャは不変。リファクタリング後に `AuthService.refresh` 内で `User`（本タスクの作業対象）に対して呼ぶメソッドを確定する。

`AuthService.refresh` のシグネチャは不変:
- メソッド名: `refresh`
- 引数: `@NonNull RefreshCommand command`
- 戻り値: `RefreshResult`

リファクタリング後に `AuthService.refresh` 内で `User` に対して呼ぶメソッド（＝本タスクの作業対象）:
- メソッド名: `isActive`
- 引数: なし
- 戻り値: `boolean`

---

## 作業対象メソッドのシグネチャ

作業概要を達成するための中心メソッド（`User#isActive`）。

- メソッド名: `isActive`
- 引数: なし
- 戻り値: `boolean`（`userIdIsActive && tenantIdIsActive` を返す。例外はスローしない）

---

## 内側レイヤーへの契約（条件: 作業対象が ValueObject でも Repository でもない場合）

該当なし。`isActive()` は既存 record コンポーネント（`userIdIsActive` / `tenantIdIsActive`、いずれも `boolean`）を参照するのみで、内側レイヤー（infrastructure 等）や他の DomainObject / ValueObject への新規呼び出しは発生しない。スタブの新規作成は不要。

---

## ドメインモデルの利用（条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか）

プリミティブ型への依存を禁止する方針だが、本タスクは新規のプリミティブ依存を追加しない。`isActive()` が参照する `userIdIsActive` / `tenantIdIsActive` は既存 record（`User`）が既に保持しているプリミティブ boolean コンポーネントであり、本タスクではその参照方法（呼び出し元をメソッド経由に集約する）のみを変更する。新規に作成する DomainObject / ValueObject はない。

- 作成する DomainObject / ValueObject 名: なし（新規作成なし。既存の `User` record および既存コンポーネントのみを利用する）

---

## 変更対象ファイル一覧（本タスクが操作するすべてのファイル）

| 操作 | ファイルパス（`src/` 以下） | 内容 |
|----|-----------------|----|
| 変更 | `main/.../domain/model/User.java` | `public boolean isActive()` を追加し、`authenticate` 内の先頭 2 つの if を `if (!isActive())` に置換 |
| 変更 | `main/.../application/AuthService.java` | `refresh` 内の `if (!user.userIdIsActive() || !user.tenantIdIsActive())` を `if (!user.isActive())` に置換 |
| 変更 | `test/.../domain/model/UserTest.java` | `isActive()` の正常系・異常系テストを追記（`authenticate` 経由の既存テストは維持） |
| 変更（必要な場合のみ） | `test/.../application/AuthServiceTest.java` | 振る舞い不変のため既存の `refresh` 系テストはモック設定の変更不要な想定。念のため green であることを確認する |
