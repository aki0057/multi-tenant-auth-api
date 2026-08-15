# tasklist

## 前提確認・TODO.md 準備

- [x] `User#isActive` が TODO.md の DomainObject (domain) 章に存在しないため、空のチェックボックスで `User#isActive` を追記する

## User ドメインオブジェクトへの isActive 追加（本タスクの主軸）

- [x] `domain/model/User.java`（record）に `public boolean isActive()`（`userIdIsActive && tenantIdIsActive` を返す）を追加する
- [x] `isActive()` に Javadoc を記載する
- [x] `User#authenticate(RawPassword, PasswordVerifier)` 内の先頭 2 つの if（`!userIdIsActive` チェック・`!tenantIdIsActive` チェック）を `if (!isActive())` の 1 つに置き換える（例外は従来どおり `AuthenticationFailedException`）
- [x] `authenticate` の Javadoc（挙動説明）に矛盾がないか確認し、必要に応じて更新する
- [x] `UserTest.java` に `isActive()` の正常系テストを記載する（例: `isActive_true`: 両フラグ有効 → `true`）
- [x] `UserTest.java` に `isActive()` の異常系（無効系）テストを記載する（例: `isActive_userInactive`: `userIdIsActive=false` → `false`、`isActive_tenantInactive`: `tenantIdIsActive=false` → `false`）
- [x] 既存の `authenticate_userInactive` / `authenticate_tenantInactive` / `authenticate_success` / `authenticate_wrongPassword` テストが `isActive()` 経由のリファクタリング後も green のまま通ることを確認する

## AuthService の呼び出し箇所変更

- [x] `application/AuthService.java` の `refresh(RefreshCommand)` 内の `if (!user.userIdIsActive() || !user.tenantIdIsActive())` を `if (!user.isActive())` に置き換える（例外は従来どおり `InvalidRefreshTokenException`）
- [x] `refresh` の Javadoc（挙動説明）に矛盾がないか確認し、必要に応じて更新する
- [x] 既存の `AuthServiceTest.java` の `refresh` 系テスト（ユーザー無効・テナント無効のケースを含む）が変更後も green のまま通ることを確認する（振る舞い不変のためモック設定の変更は不要な想定。必要であれば最小限の修正を行う）

## TODO.md の最終更新

- [x] `User#isActive` の TODO.md（DomainObject (domain) 章）のチェックボックスを埋める（完了にする）
