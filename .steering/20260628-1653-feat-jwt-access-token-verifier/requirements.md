# requirements

## 作業概要

`StubAccessTokenVerifier` を削除し、jjwt (HS256) を用いた `JwtAccessTokenVerifier` として本実装する。
発行側 `JwtAccessTokenProvider`（増分1で実装済み）と同一の `JwtProperties` / `SecretKey` 生成方法を用いて、
`AccessTokenVerifier#verify` を実装する。これにより `JwtAuthenticationFilter` が実トークンを検証して
`SecurityContext` を構築できるようになる。
堅牢化スコープとして `parseSignedClaims` による alg=none 未署名トークン拒否（`UnsupportedJwtException` 伝播）を含む。
jjwt がスローする例外は try/catch せず、そのまま呼び出し元（フィルタ側 C-2）へ伝播させる。

## 作業対象レイヤー

infrastructure (security)

## 作業対象の種別

infrastructure.security

## 使用するテスト・フレームワーク等

- テストフレームワーク: プレーン JUnit 5（Spring 未起動・`@SpringBootTest` なし・直接 `new`）
- Spring MVC アノテーション: なし

## 隣接レイヤー

- 1 つ外側のレイヤー: domain/service（`AccessTokenVerifier` インターフェース）
- 1 つ内側のレイヤー: なし（`io.jsonwebtoken:jjwt-api:0.13.0` を直接使用するためスタブ不要）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)

外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる。

- インターフェース: `io.github.aki0057.multitenant.auth.domain.service.AccessTokenVerifier`
- メソッド名: `verify`
- 引数: `String token`
- 戻り値: `AuthenticatedUser`

## 作業対象メソッドのシグネチャ

作業概要を達成するための単一メソッド。

- クラス: `JwtAccessTokenVerifier implements AccessTokenVerifier`（`@Component`、`infrastructure/security/` パッケージ）
- メソッド名: `verify`
- 引数: `String token`
- 戻り値: `AuthenticatedUser`
- 補足: jjwt の例外（`SignatureException`、`ExpiredJwtException`、`MalformedJwtException`、`UnsupportedJwtException` 等）は try/catch せずそのまま伝播する。`parseSignedClaims` を用いることで alg=none 未署名トークンは `UnsupportedJwtException` で自動拒否される。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)

内側は `jjwt-api:0.13.0` ライブラリを直接使用する。カスタムクラスの新規作成・スタブは不要。

- スタブ新規作成: なし

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)

プリミティブ型への依存を禁止する。以下の既存 DomainObject / ValueObject を利用する（新規作成なし）。

| クラス | パッケージ | 利用方法 | 型 |
|---|---|---|---|
| `AuthenticatedUser` | `domain/model/` | `verify` の戻り値として構築 | record（`UserId`, `TenantId`, `Role`） |
| `UserId` | `domain/model/vo/` | `Long.parseLong(claims.getSubject())` から生成 | `Long` |
| `TenantId` | `domain/model/vo/` | `claims.get("tenantId", Long.class)` から生成 | `Long` |
| `Role` | `domain/model/vo/` | `claims.get("role", String.class)` から生成 | `String` |

- 作成する DomainObject / ValueObject 名: なし（既存利用のみ）

---

## 新規作成ファイル詳細

### 1. `JwtAccessTokenVerifier`（@Component）

- パッケージ: `io.github.aki0057.multitenant.auth.infrastructure.security`
- アノテーション: `@Component`
- 依存: `JwtProperties`（コンストラクタインジェクション）
- コンストラクタ: `JwtAccessTokenProvider` をミラーし、`Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8))` で `SecretKey` を構築する（発行/検証で鍵生成方法を一致させる）
- `verify(String token)` 実装概要:
  - ライブラリ API: `io.jsonwebtoken.Jwts`（0.13.0）
  - パース: `Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)`
  - `parseSignedClaims` を使用することで alg=none 未署名トークンは `UnsupportedJwtException` で自動拒否される（alg=none 拒否スコープを満たす）
  - 例外伝播: jjwt の例外を try/catch せず、そのまま呼び出し元へ伝播する
  - クレーム復元:
    - `UserId` = `new UserId(Long.parseLong(claims.getSubject()))`
    - `TenantId` = `new TenantId(claims.get("tenantId", Long.class))`
    - `Role` = `new Role(claims.get("role", String.class))`
  - 返却: `new AuthenticatedUser(userId, tenantId, role)`

### 2. 削除対象

- `StubAccessTokenVerifier.java`（`infrastructure/security/` パッケージ）— `@Component` の Bean 重複を避けるため削除する

### 3. 微修正対象

- `AccessTokenVerifier.java`（`domain/service/` パッケージ）— 末尾の `// TODO: 増分2...` コメント（21行目）を削除する

### 4. テストクラス

- クラス名: `JwtAccessTokenVerifierTest`
- パッケージ: `io.github.aki0057.multitenant.auth.infrastructure.security`（テストソースツリー）
- テスト種別: プレーン JUnit 5（Spring 未起動、`JwtAccessTokenProvider` と `JwtAccessTokenVerifier` を直接 `new`）
- テストデータ: `JwtProperties` を直接インスタンス化（`application-test.properties` と同じダミーシークレット）
- テストメソッド命名: `verify_条件()`、DisplayName 日本語（正常系: / 異常系:）
- 正常系:
  - `JwtAccessTokenProvider#issue` で発行したトークンを `JwtAccessTokenVerifier#verify` で検証し、`userId` / `tenantId` / `role` が発行時の値と一致すること
- 異常系:
  - 別の秘密鍵で発行したトークンを `verify` → `SignatureException` がスロー（伝播）されること
  - 有効期限切れのトークンを `verify` → `ExpiredJwtException` がスロー（伝播）されること
  - 不正フォーマット文字列を `verify` → `MalformedJwtException` 等がスロー（伝播）されること
  - alg=none の未署名トークンを `verify` → `UnsupportedJwtException` がスロー（伝播）されること

---

## TODO.md との対応

以下のエントリが TODO.md に存在する。実装完了後にチェックを埋める。

- `infrastructure.security` セクション: `[ ] StubAccessTokenVerifier を JwtAccessTokenVerifier へ置換`
- `Port (domain)` セクション: `[ ] AccessTokenVerifier#verify`
