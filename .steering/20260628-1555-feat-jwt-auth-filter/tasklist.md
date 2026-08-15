# tasklist

- [x] `JwtAuthenticationFilter#doFilterInternal` を、プロジェクトルートの TODO.md の `API (presentation)` の章に、空のチェックボックスで追記する
- [x] `AuthenticatedUser`（`domain/model`）を新規作成する（`UserId userId`, `TenantId tenantId`, `Role role` を保持するバリデーションなしの軽量 record。TODO.md には登録しない）
- [x] `AccessTokenVerifier`（`domain/service`、interface）をスタブとして新規作成する（メソッドシグネチャ: `AuthenticatedUser verify(String token)`、中身は `// TODO` のみ）
- [x] `StubAccessTokenVerifier`（`infrastructure/security`、`@Component`）を一時スタブアダプタとして新規作成する（`AccessTokenVerifier` を実装、`verify` は最小の固定値返却 + `// TODO`）
- [x] `AccessTokenVerifier#verify` を TODO.md の `Port (domain)` の章に空のチェックボックスで追記する
- [x] `StubAccessTokenVerifier を JwtAccessTokenVerifier へ置換` を TODO.md の `infrastructure.security` の章に空のチェックボックスで追記する
- [x] `JwtAuthenticationFilter` のコンストラクタに `AccessTokenVerifier` を追加する（フィールドに保持し、DI で受け取る）
- [x] `SecurityConfig#jwtAuthenticationFilter()` を `AccessTokenVerifier` を引数で受け取り `JwtAuthenticationFilter` のコンストラクタへ渡すよう変更する
- [x] `JwtAuthenticationFilter#doFilterInternal` を実装する（`Authorization: Bearer <token>` ヘッダ抽出 → `verify` 呼び出し → `UsernamePasswordAuthenticationToken`（principal=`AuthenticatedUser`、credentials=`null`、authorities=`ROLE_<role>`）を SecurityContext にセット。ヘッダ無し・Bearer でない・`verify` 例外時は認証をセットせず素通り）
- [x] Javadoc を記載する（`JwtAuthenticationFilter`・`AccessTokenVerifier`・`AuthenticatedUser`・`StubAccessTokenVerifier`）
- [x] `JwtAuthenticationFilterTest` の正常系テストを記載する（有効な Bearer トークン → `AccessTokenVerifier#verify` が `AuthenticatedUser` を返す → SecurityContext に認証が載る）
- [x] `JwtAuthenticationFilterTest` の異常系テストを記載する（`Authorization` ヘッダなし → 認証がセットされず `filterChain.doFilter` が呼ばれる）
- [x] `JwtAuthenticationFilterTest` の異常系テストを記載する（`Bearer ` で始まらないヘッダ → 認証がセットされず `filterChain.doFilter` が呼ばれる）
- [x] `JwtAuthenticationFilterTest` の異常系テストを記載する（`verify` が例外をスロー → 認証がセットされず `filterChain.doFilter` が呼ばれる）
- [x] TODO.md の `JwtAuthenticationFilter#doFilterInternal` のチェックボックスを埋める（完了にする）
