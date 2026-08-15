# tasklist

- [x] 作業対象を実装する（`config/SecurityConfig#defaultSecurityFilterChain` の `authorizeHttpRequests` に `.requestMatchers("/admin/**").hasAuthority("ADMIN")` を追加する。位置は既存 `permitAll` 群の直後・`.anyRequest().authenticated()` の直前。`hasRole` は使用しない）
- [x] 既存設定を変更していないことを確認する（`@Order(1)` の `refreshSecurityFilterChain`、既存 `permitAll`（`/auth/login`・`/swagger-ui/**`・`/v3/api-docs/**`）、`authenticationEntryPoint`（401）・`accessDeniedHandler`（404）、`csrf` / `sessionManagement` / `addFilterBefore` は無変更）
- [x] Javadoc を記載する（`defaultSecurityFilterChain` の既存 Javadoc に `/admin/**` が ADMIN 限定である旨と、`hasRole` ではなく `hasAuthority` を使う理由を追記する。既存記述は削らない）
- [x] 正常系のテストコードは新規作成しない（種別 config のため単体テストを作らない。既存 `AdminControllerTest` の正常系ケースが `Role("ADMIN")` + authority `ADMIN` で 200 を返すことにより担保される。既存テストケースを変更していないことを確認する）
- [x] 異常系のテストコードを記載する（`src/test/java/io/github/aki0057/multitenant/auth/presentation/AdminControllerTest.java` に `listTenantUsers_forbiddenForUserRole()` を 1 件追加する。`Role("USER")` + authority `USER` の principal で `GET /admin/users` を実行し、404・`$.error` == `"Not Found"`・`verify(userService, never()).listTenantUsers(any())` を検証する。`@DisplayName` は先頭に `異常系:` を付ける）
