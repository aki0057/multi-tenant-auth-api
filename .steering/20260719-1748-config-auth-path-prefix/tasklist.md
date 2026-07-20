# tasklist

（種別 config のため、TODO.md への登録・内側スタブ作成・DomainObject / ValueObject 作成の各条件付きタスクはいずれも非該当としてスキップする。単体テストは新設せず、既存の結合テストのパス検証を新パスへ追随させて回帰担保する。）

- [x] `presentation/AuthController.java` の `@PostMapping("/login")` を `@PostMapping("/auth/login")` へ変更する
- [x] `presentation/AuthController.java` の `@PostMapping("/refresh")` を `@PostMapping("/auth/refresh")` へ変更する
- [x] `presentation/AuthController.java` の refreshToken クッキー `.path("/refresh")` を `.path("/auth/refresh")` へ変更する
- [x] `presentation/AuthController.java` の Javadoc / `@Operation` / `@Parameter` 記述内の `/login`・`/refresh`・`Path=/refresh` を新パスへ更新する
- [x] `config/SecurityConfig.java` の `.securityMatcher("/refresh")` を `.securityMatcher("/auth/refresh")` へ変更する
- [x] `config/SecurityConfig.java` の `.requestMatchers("/login").permitAll()` を `.requestMatchers("/auth/login").permitAll()` へ変更する
- [x] `config/SecurityConfig.java` の Javadoc 内の `/login`・`/refresh` 記述を新パスへ更新する
- [x] `config/OpenApiConfig.java` の Javadoc 内の `/login`・`/refresh` 記述を新パスへ更新する
- [x] `test/.../presentation/AuthControllerTest.java` の `post("/login")`・`post("/refresh")`・`cookie().path("refreshToken", ...)` 検証を新パスへ更新する
- [x] `test/.../LoginIntegrationTest.java` の `post("/login")`・クッキー Path 検証を新パスへ更新する
- [x] `README.md` の `Path=/refresh` および CSRF 関連の記述を新パスへ更新する
