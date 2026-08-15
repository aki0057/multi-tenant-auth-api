# requirements

## 作業概要
`config/SecurityConfig` の `defaultSecurityFilterChain`（`@Order(2)`）の `authorizeHttpRequests` に、`/admin/**` を ADMIN ロール限定とする認可ルールを追加する。

直前の steering `.steering/20260815-1714-feat-admin-endpoint/` で presentation 層の `AdminController#listTenantUsers`（`GET /admin/users`）を実装したが、認可設定は意図的にスコープ外としていた。本 steering でその認可設定を追加し、`/admin/**` 配下を ADMIN 以外から利用できないようにする。

ユーザー決定事項（変更禁止）:
- 追加するルールは `requestMatchers("/admin/**").hasAuthority("ADMIN")` とする。**`hasRole` は使用禁止**。
  - 理由: `presentation/filter/JwtAuthenticationFilter` は `new SimpleGrantedAuthority(authenticatedUser.role().value())` をセットしており、authority の値は `"ADMIN"` / `"USER"`（`ROLE_` 接頭辞なし）である。`hasRole("ADMIN")` は `ROLE_ADMIN` という authority を前提とするため一致せず、ADMIN ユーザーまで拒否されてしまう。したがって `hasAuthority("ADMIN")` を用いる。
- ルールの記述位置は `anyRequest().authenticated()` より**前**とする（Spring Security のマッチャは先勝ちのため、後ろに置くと `anyRequest()` が先にマッチしてルールが効かない）。既存の `permitAll` 群（`/auth/login`、`/swagger-ui/**`、`/v3/api-docs/**`）との前後関係はパスが重複しないため任意だが、`permitAll` 群の直後・`anyRequest()` の直前に置く。
- 認可失敗時のレスポンスは既存の `accessDeniedHandler` により **403 ではなく 404 Not Found + `{"error": "Not Found"}`** となる。この既存挙動は変更しない。
- 次の既存設定は一切変更しない。
  - `@Order(1)` の `refreshSecurityFilterChain`（`/auth/refresh`・`/auth/logout`）
  - `csrf` 無効化・`sessionManagement`（STATELESS）
  - `exceptionHandling` の `authenticationEntryPoint`（401）・`accessDeniedHandler`（404）
  - 既存の `permitAll` 設定（`/auth/login`、`/swagger-ui/**`、`/v3/api-docs/**`）
  - `addFilterBefore` による `JwtAuthenticationFilter` の挿入
- Javadoc: `defaultSecurityFilterChain` の既存 Javadoc に、`/admin/**` が ADMIN 限定である旨を追記する（既存記述は削らない）。
- `JwtAuthenticationFilter`・`AdminController`・`UserService` など `config/` 以外のソースは変更しない。

## 作業対象レイヤー
config（横断的関心事）

## 作業対象の種別
config

## 使用するテスト・フレームワーク等
- テストフレームワーク: 単体テストは作成しない（`docs/testing-guidelines.md` の「② テストを作成しない種別」に従い、Spring 設定クラス（`@Configuration`）は結合テストで間接的に検証する）。
  - ただし既存の `src/test/java/io/github/aki0057/multitenant/auth/presentation/AdminControllerTest.java` は `@WebMvcTest(AdminController.class)` + `@Import({SecurityConfig.class, PasswordEncoderConfig.class, GlobalExceptionHandler.class})` により実物の `SecurityConfig` を読み込むため、本設定変更の効果を同ファイルで検証できる。同ファイルへ**異常系テストを 1 件追加**する（詳細は「テスト方針」）。
- Spring MVC アノテーション: なし（既存 `@Configuration` / `@EnableWebSecurity` クラスへの設定変更）。使用する Spring Security API は `AuthorizeHttpRequestsConfigurer` の `requestMatchers(...).hasAuthority(String)`。
- テスト側で使用するもの（既存 `AdminControllerTest` に準拠）: JUnit 5、Mockito（`@MockitoBean`）、`SecurityMockMvcRequestPostProcessors.authentication(...)`、MockMvc の `jsonPath`。

## 隣接レイヤー
- 1 つ外側のレイヤー: 該当なし（config のため。特定の業務レイヤーに属さない）
- 1 つ内側のレイヤー: 該当なし（config のため）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
該当なし（config のため）。DDD 契約項目（外側／内側レイヤーとの契約、スタブ / Command / DomainObject / ValueObject の作成、TODO.md 登録）はいずれも発生しない。

## 作業対象メソッドのシグネチャ
既存メソッドの設定変更であり、新規メソッドのシグネチャ追加はない。
- 対象: `SecurityConfig#defaultSecurityFilterChain(HttpSecurity http, AccessTokenVerifier accessTokenVerifier)`（`@Order(2)`・戻り値 `SecurityFilterChain`・`throws Exception`）。シグネチャは変更しない。
- 変更内容: 同メソッドの `authorizeHttpRequests` ラムダ内へ `.requestMatchers("/admin/**").hasAuthority("ADMIN")` を追加する。記述位置は既存の `permitAll` 群の直後・`.anyRequest().authenticated()` の直前とする。
- 変更後の認可ルールの順序（期待形）:
  1. `.requestMatchers("/auth/login").permitAll()`（既存・変更しない）
  2. `.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()`（既存・変更しない）
  3. `.requestMatchers("/admin/**").hasAuthority("ADMIN")`（**今回追加**）
  4. `.anyRequest().authenticated()`（既存・変更しない）
- Javadoc: `defaultSecurityFilterChain` の既存 Javadoc に「`/admin/**` は ADMIN ロール（authority `ADMIN`）限定」である旨と、`hasRole` ではなく `hasAuthority` を使う理由（`JwtAuthenticationFilter` が `ROLE_` 接頭辞なしの authority をセットするため）を追記する。

### 期待する挙動（受け入れ条件）
- authority `ADMIN` を持つ認証済みリクエストの `GET /admin/users` は 200 OK（既存の正常系テストが引き続き green）。
- authority `USER` を持つ認証済みリクエストの `GET /admin/users` は、`accessDeniedHandler` により **404 Not Found**・ボディ `{"error": "Not Found"}` を返し、`UserService#listTenantUsers` は呼ばれない。
- 未認証リクエストの `GET /admin/users` は既存どおり `authenticationEntryPoint` により 401 Unauthorized・ボディ `{"error": "Unauthorized"}`（既存テスト `listTenantUsers_unauthenticated()` が引き続き green）。
- `/auth/login`・Swagger 系・`/auth/refresh`・`/auth/logout` の挙動は変更されない。

### テスト方針
- 種別 config のため単体テスト（`SecurityConfigTest` 等）は作成しない。
- 既存 `src/test/java/io/github/aki0057/multitenant/auth/presentation/AdminControllerTest.java` へ**異常系テストを 1 件だけ追加**する。
  - メソッド名: `listTenantUsers_forbiddenForUserRole()`（`docs/testing-guidelines.md` の `methodName_condition()` 規約に従う）
  - `@DisplayName`: 「異常系: USER ロールで GET /admin/users を呼ぶと 403 ではなく 404 Not Found が返る。」（先頭に `異常系:` を付ける）
  - 内容: `new AuthenticatedUser(new UserId(...), new TenantId(...), new Role("USER"))` を principal に持つ `UsernamePasswordAuthenticationToken`（authority は既存の `adminToken()` に倣い `new SimpleGrantedAuthority("USER")`）を `SecurityMockMvcRequestPostProcessors.authentication(...)` で付与して `GET /admin/users` を実行し、
    - `status().isNotFound()`
    - `jsonPath("$.error").value("Not Found")`
    - `verify(userService, never()).listTenantUsers(any())`（`UserService#listTenantUsers` が呼ばれないこと）
    を検証する。
  - 補助メソッドとして `adminToken()` に倣う `userToken()` を追加してよい（Javadoc を付ける）。
- 既存テストケース（`listTenantUsers_success` ほか正常系 6 件・`listTenantUsers_unauthenticated`）は**変更しない**。正常系は既存ケースが `Role("ADMIN")` + authority `ADMIN` の principal を使っており、本変更後も 200 が返ることで担保される。
- 段階4では、既存の全テスト（195 件）＋追加 1 件が green であることを確認する。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
該当なし（config のため）。内側スタブの作成・`TODO.md` への登録は発生しない。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
該当なし（config のため）。DomainObject / ValueObject の新規作成は発生しない（テストで既存の `AuthenticatedUser` / `UserId` / `TenantId` / `Role` を組み立てて使うのみ）。
