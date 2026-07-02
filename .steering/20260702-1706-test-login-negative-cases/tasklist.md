# tasklist

- [x] `src/test/java/io/github/aki0057/multitenant/auth/LoginIntegrationTest.java` に、パスワード不一致 → 401 を検証するテストメソッド（`login_withWrongPassword_returns401`、`@DisplayName("異常系: ...")`）を追加する。既存 `setUp()` のテストデータ（`testTenant` / `test@example.com`）に対し、誤ったパスワードを送信し `status().isUnauthorized()` を検証する
- [x] 同ファイルに、ユーザー不在 → 401 を検証するテストメソッド（`login_withNonExistentUser_returns401`）を追加する。既存 `setUp()` のテナント（`testTenant`）に対し、未登録の email を送信し `status().isUnauthorized()` を検証する
- [x] 同ファイルに、アカウント無効 → 401 を検証するテストメソッド（`login_withInactiveAccount_returns401`）を追加する。テストメソッド内で `jdbcTemplate` により `users.is_active = false` の追加ユーザー（既存とは別 email、同一テナント）を投入し、そのユーザーの email + 正しいパスワードで送信し `status().isUnauthorized()` を検証する
- [x] 同ファイルに、テナント無効 → 401 を検証するテストメソッド（`login_withInactiveTenant_returns401`）を追加する。テストメソッド内で `jdbcTemplate` により `tenants.is_active = false` の追加テナントと、その配下に `is_active = true` の追加ユーザーを投入し、そのテナントコード + email + 正しいパスワードで送信し `status().isUnauthorized()` を検証する
- [x] 同ファイルに、必須項目欠落（`@NotBlank`）→ 400 を検証するテストメソッド（`login_withMissingRequiredField_returns400`）を追加する。JSON ボディから必須フィールド（`password`）を欠落させて送信し `status().isBadRequest()` を検証する
- [x] 追加した5メソッドすべてに `testing-guidelines.md` の命名規則（`methodName_condition()`）に沿ったメソッド名と、`@DisplayName`（先頭に `異常系:` を付けた日本語）を記載する
- [x] 追加したテストデータ投入（`jdbcTemplate.update`）が、既存クラスの `@Transactional` によるロールバック方針に従っており、手動 DELETE を追加していないことを確認する
- [x] `src/main/` 配下の本体実装（`AuthController` / `AuthService` / `User` / `LoginRequest` 等）を変更していないことを確認する（本タスクはテスト追加のみ）
