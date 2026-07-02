# tasklist

- [x] 作業対象を実装する（`AuthService#login` の入口 3 VO 生成を try/catch で囲み、`IllegalArgumentException` を `BadCredentialsException("Invalid credentials")` に変換する。既存の `GlobalExceptionHandler` はそのまま再利用し、presentation 層への新規例外ハンドラは追加しない。`LoginRequest` への `@Pattern`/`@Size` も追加しない）
- [x] Javadoc を記載する（`login` メソッドの Javadoc `@throws` 説明に、VO 形式不正の場合も `BadCredentialsException` を投げる旨を追記する）
- [x] 正常系のテストコードを記載する（既存の `AuthServiceTest#login_success` および `LoginIntegrationTest#login_withValidCredentials_returns200` が本改修後も green であることを確認する。挙動変更がないため新規追加は不要）
- [x] 異常系のテストコードを記載する
  - `AuthServiceTest` に、tenantCode・email・password それぞれが形式不正な `LoginCommand`（例: tenantCode に記号、email がドット無し `a@b`、password が8文字未満）で `login()` が `BadCredentialsException` を投げ、`accessTokenProvider.issue` が呼ばれないことを検証するテストを追加する
  - `LoginIntegrationTest` に、形式不正な資格情報を POST /login した際に HTTP 401 が返ることを検証する e2e テストを追加する（既存の正常系 200 e2e と同じ構成に倣う）
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックスを埋める（完了にする）（`## Service (application)` 章の `AuthService#login(LoginCommand) の JWT アクセストークン発行` は既に `[x]` 済みであることを確認する。新規スタブ・新規 DomainObject/ValueObject を作成していないため、TODO.md への新規追記は不要）
