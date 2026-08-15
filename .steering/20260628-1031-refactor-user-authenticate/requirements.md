# requirements

## 作業概要

AuthService が application 層で直接行っている 3 つの認証検証（①アカウント無効確認 `!user.userIdIsActive()`、②テナント無効確認 `!user.tenantIdIsActive()`、③パスワード不一致確認 `passwordEncoder.matches(...)`）を、User ドメインオブジェクトへ移譲するリファクタリング。

具体的には次の変更を行う。

1. ドメインポート `PasswordVerifier`（`domain/service/`）を新規作成する（`boolean matches(RawPassword, PasswordHash)`）。User を Spring 非依存に保つためのインターフェース。
2. ドメイン例外 `AuthenticationFailedException`（`domain/exception/`）を新規作成する（RuntimeException 継承・Spring 非依存）。
3. `User` に認証メソッド `authenticate(RawPassword rawPassword, PasswordVerifier passwordVerifier)` を追加し、3 検証を内部で行いいずれか失敗で `AuthenticationFailedException` を throw する。
4. `PasswordEncoderVerifier`（`infrastructure/security/`）を新規作成する（`PasswordVerifier` の BCrypt 実装。`@Component`・`@RequiredArgsConstructor` で既存の `PasswordEncoder` Bean に委譲）。
5. `AuthService` のフィールドを `PasswordEncoder` → `PasswordVerifier` へ差し替え、3 つの if-throw ブロックを `user.authenticate(rawPassword, passwordVerifier)` の 1 呼び出し + `AuthenticationFailedException` → `BadCredentialsException` 変換の try-catch へ置換する。

外部挙動（HTTP 401 / レスポンス）は不変。user-not-found 経路・`GlobalExceptionHandler`・`LoginIntegrationTest` は変更しない。

**注意**: 本ワークフローは本来「外側→内側へ単一レイヤーを新規構築する」前提だが、今回は既存実装の責務移譲であり、複数レイヤー（domain の DomainObject・service ポート・exception／application の Service／infrastructure/security のアダプタ）にまたがるリファクタリングである。中心となる作業対象を `User#authenticate` の追加と定め、その軸で以下を整理する。

---

## 作業対象レイヤー

domain（DomainObject）を主軸とした複数レイヤーリファクタリング。付随して domain/service（ドメインポート新設）・domain/exception（新設）・application（Service 変更）・infrastructure/security（アダプタ新設）を含む。

## 作業対象の種別

DomainObject（主軸）

---

## 使用するテスト・フレームワーク等

- テストフレームワーク: JUnit（`UserTest` は Spring コンテキスト不要のため JUnit のみ）、Mockito（`AuthServiceTest` はモックを使用）
- Spring MVC アノテーション: なし（domain / application 層のテストのみ）

---

## 隣接レイヤー

- 1 つ外側のレイヤー: application（`AuthService`）
- 1 つ内側のレイヤー: domain/service（`PasswordVerifier`、ドメインポートインターフェース）

---

## 外側レイヤーとの契約（条件: 作業対象が API でない場合）

外側（`AuthService`）は変更対象でもある既存メソッドのため二重の記述になるが、リファクタリング後に `AuthService` が `User` に対して行う呼び出しのシグネチャを確定する。

`AuthService.login` のシグネチャは不変:
- メソッド名: `login`
- 引数: `@NonNull LoginCommand command`
- 戻り値: `String`

リファクタリング後に `AuthService.login` 内で `User` に対して呼ぶメソッド（＝本タスクの作業対象）:
- メソッド名: `authenticate`
- 引数: `RawPassword rawPassword, PasswordVerifier passwordVerifier`
- 戻り値: `void`

---

## 作業対象メソッドのシグネチャ

作業概要を達成するための中心メソッド（`User#authenticate`）。

- メソッド名: `authenticate`
- 引数: `RawPassword rawPassword, PasswordVerifier passwordVerifier`
- 戻り値: `void`（認証失敗時は `AuthenticationFailedException` を throw）

---

## 内側レイヤーへの契約（条件: 作業対象が ValueObject でも Repository でもない場合）

`User#authenticate` が依存するドメインポートを本タスクで新設する。標準パターンと異なり、スタブとして残さず本タスクで完成させる（`PasswordEncoderVerifier` が実装を提供するため）。

- インターフェース: `PasswordVerifier`（`domain/service/` に新規作成）
- メソッド名: `matches`
- 引数: `RawPassword rawPassword, PasswordHash passwordHash`
- 戻り値: `boolean`

---

## ドメインモデルの利用（条件: 作業対象が Service / DomainObject / infrastructure.mapper のいずれか）

プリミティブ型への依存を禁止する。`User#authenticate` の引数はすべて既存 ValueObject（`RawPassword`・`PasswordHash` は `domain/model/vo/` に実在）。

- 変更対象 DomainObject: `User`（`domain/model/User.java` に instance method を追加）
- 新規作成ドメインポート: `PasswordVerifier`（`domain/service/`、Spring 非依存インターフェース、本タスクで完成）
- 新規作成ドメイン例外: `AuthenticationFailedException`（`domain/exception/`、`RuntimeException` 継承、Spring 非依存、本タスクで完成）

---

## 変更対象ファイル一覧（本タスクが操作するすべてのファイル）

| 操作   | ファイルパス（`src/main/java/.../` 以下）                             | 内容                                                              |
|------|--------------------------------------------------------------|-----------------------------------------------------------------|
| 新規作成 | `domain/service/PasswordVerifier.java`                       | ドメインポートインターフェース。`matches(RawPassword, PasswordHash): boolean`    |
| 新規作成 | `domain/exception/AuthenticationFailedException.java`        | ドメイン例外。`RuntimeException` 継承。Spring 非依存。                        |
| 変更   | `domain/model/User.java`                                     | `authenticate(RawPassword, PasswordVerifier): void` を追加         |
| 新規作成 | `infrastructure/security/PasswordEncoderVerifier.java`       | `PasswordVerifier` の BCrypt 実装。既存 `PasswordEncoder` Bean に委譲      |
| 変更   | `application/AuthService.java`                               | フィールド差し替え・3 if-block → `user.authenticate` + try-catch へ置換     |
| 新規作成 | テスト: `domain/model/UserTest.java`                           | `authenticate` の正常系・異常系テスト（JUnit のみ。`PasswordVerifier` はラムダスタブ） |
| 変更   | テスト: `application/AuthServiceTest.java`                     | `@Mock PasswordEncoder` → `@Mock PasswordVerifier` 差し替え         |
