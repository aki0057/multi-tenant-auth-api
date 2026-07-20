# tasklist

- [x] 作業対象を実装する（`src/test/java/io/github/aki0057/multitenant/auth/LogoutIntegrationTest.java` を作成する。`@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` + `@Transactional`。`@BeforeEach` / `@AfterEach` で `JdbcTemplate` により tenants / users を INSERT / DELETE し、`PasswordEncoder#encode` でハッシュを実行時生成する。`LoginIntegrationTest` の構成・命名に合わせる）
- [x] Javadoc を記載する（テストクラスの概要として、`POST /auth/logout` の結合テスト（HTTP 入口から DB のリフレッシュトークン失効まで／冪等・CSRF 保護）である旨を記述する）
- [x] 正常系のテストコードを記載する（例: (1) 先に `POST /auth/login` で `refreshToken` / `XSRF-TOKEN` クッキーを取得し、CSRF ヘッダ付きで `POST /auth/logout` を呼ぶと 204・`refreshToken` 削除クッキー付与・DB の該当行 `is_revoked = true`。(2) `refreshToken` クッキー未提示でも CSRF 有効なら 204（冪等）。(3) DB 未一致の未知トークンでも 204（冪等・エラーにしない））
- [x] 異常系のテストコードを記載する（CSRF トークン（`X-XSRF-TOKEN` ヘッダ / `XSRF-TOKEN` クッキー）が無い・不一致の場合は 403 Forbidden が返る）
