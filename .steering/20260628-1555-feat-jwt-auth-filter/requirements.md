# requirements

## 作業概要

`JwtAuthenticationFilter`（`presentation/filter`）の実装を、常に dev-user をセットするダミーから、JWT 検証結果を SecurityContext に投入する実装へ置換する。
フィルタが呼び出すドメインポート `AccessTokenVerifier`（`domain/service`）、その戻り値として使う認証情報キャリア `AuthenticatedUser`（`domain/model`）、および Spring コンテキスト起動を維持するための一時スタブアダプタ `StubAccessTokenVerifier`（`infrastructure/security`）をあわせて新規作成する。

## 作業対象レイヤー

presentation/filter

## 作業対象の種別

API (presentation)

## 使用するテスト・フレームワーク等

- テストフレームワーク: JUnit 5 (Jupiter) + Mockito
- Spring MVC アノテーション: なし。Spring 未起動（`@ExtendWith(MockitoExtension.class)` を使用）。`MockHttpServletRequest` / `MockHttpServletResponse` / `MockFilterChain` を使用してフィルタを直接呼び出す。

## 隣接レイヤー

- 1 つ外側のレイヤー: Spring Security フィルタチェーン（フレームワーク）
- 1 つ内側のレイヤー: domain/service（`AccessTokenVerifier` ポート）

## 作業対象メソッドのシグネチャ

`OncePerRequestFilter#doFilterInternal` のオーバーライド。現状のダミー実装を JWT 検証実装へ置換する。

- メソッド名: `doFilterInternal`
- 引数: `@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, FilterChain filterChain`
- 戻り値: `void`（throws `ServletException, IOException`）

## 内側レイヤーへの契約

内側はまだ存在しないため、スタブとして新規作成する（実装しない・TODO のみ）。

### AccessTokenVerifier（domain/service、interface、スタブ）

- メソッド名: `verify`
- 引数: `String token`
- 戻り値: `AuthenticatedUser`
- 備考: 既存の `AccessTokenProvider` とは別ポート（責務分離。`AccessTokenProvider#issue` にメソッド追加はしない）。HS256 パースなどの具体的な検証処理は増分2（`infrastructure/security` の `JwtAccessTokenVerifier`）の責務であり、この増分のスコープ外。

### AuthenticatedUser（domain/model、軽量 VO）

- 保持フィールド: `UserId userId`、`TenantId tenantId`、`Role role`
- バリデーションを一切持たない。DB の `User` 集約を復元しない。クレームから構築できる最小情報のみを保持する record。
- Command と同様に作成と同時に完成扱いとし、TODO.md には登録しない。

### StubAccessTokenVerifier（infrastructure/security、`@Component`、一時スタブアダプタ）

- `AccessTokenVerifier` を実装する一時スタブ Bean。
- `verify` の中身は `// TODO` のみ（最小の固定値返却で可）。
- `@SpringBootTest` によるフルコンテキスト起動が具象 Bean を要求するため必要（issue 側の前例と同種の問題）。
- 増分2で `JwtAccessTokenVerifier` 実装へ置換する。TODO.md の `infrastructure.security` の章に空チェックボックスで登録する。

## SecurityConfig の変更

`JwtAuthenticationFilter` のコンストラクタに `AccessTokenVerifier` を追加するため、`SecurityConfig#jwtAuthenticationFilter()` に `AccessTokenVerifier` を DI して渡すよう変更する。変更対象は `config/SecurityConfig.java`。

## セキュリティポリシー（フィルタが担う挙動）

- C-1: トークンは `Authorization: Bearer <token>` ヘッダから取得する。ヘッダが存在しない場合・`Bearer ` で始まらない場合は認証をセットせず素通り（`filterChain.doFilter` を呼ぶ）。
- C-2: `verify` が例外をスローした場合（署名不正・期限切れ・パース失敗等）はフィルタ内で例外を再送出せず、認証をセットせずに素通りさせる。認可は `SecurityConfig` 既存の `authenticated()` + entryPoint による 401 に委ねる。
- C-3/C-4（HS256 固定・alg=none 拒否・leeway）: この増分の対象外（増分2で実装）。

## SecurityContext への載せ方

- principal: `AuthenticatedUser` インスタンス
- credentials: `null`
- authorities: `List.of(new SimpleGrantedAuthority("ROLE_" + authenticatedUser.role().value()))`
- `UsernamePasswordAuthenticationToken` の 3 引数コンストラクタを使い、`setDetails` は不要。
