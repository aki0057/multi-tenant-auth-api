# requirements

## 作業概要
`StubAccessTokenProvider` を削除し、jjwt (HS256) を用いた `JwtAccessTokenProvider` として本実装する。
合わせて `JwtProperties`（`@ConfigurationProperties` record）を新規作成し、`jwt.secret` / `jwt.expiration` をバインドする。
`JwtProperties` の Bean 有効化は `infrastructure/security/` 内に新設する `@Configuration` クラス（`JwtConfig`）で行い、`@EnableConfigurationProperties(JwtProperties.class)` を付与する。
スコープは発行のみ。検証（verify/parse）・`JwtAuthenticationFilter` は対象外。
infrastructure.security 以外の prod コードには一切触れない。

## 作業対象レイヤー
infrastructure (security)

## 作業対象の種別
infrastructure.security

## 使用するテスト・フレームワーク等
- テストフレームワーク: プレーン JUnit 5（`@SpringBootTest` なし・Spring コンテキスト不使用）
- Spring MVC アノテーション: なし

## 隣接レイヤー
- 1 つ外側のレイヤー: domain/service（`AccessTokenProvider` インターフェース）
- 1 つ内側のレイヤー: なし（`io.jsonwebtoken:jjwt-api:0.13.0` を Maven 依存として直接使用するためスタブ不要）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる。

- インターフェース: `io.github.aki0057.multitenant.auth.domain.service.AccessTokenProvider`
- メソッド名: `issue`
- 引数: `User user`
- 戻り値: `String`

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド。

- クラス: `JwtAccessTokenProvider implements AccessTokenProvider`（`@Component`、`infrastructure/security/` パッケージ）
- メソッド名: `issue`
- 引数: `User user`
- 戻り値: `String`（HS256 署名済み JWT 文字列）

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
内側は `jjwt-api:0.13.0` ライブラリを直接使用する。カスタムクラスの新規作成・スタブは不要。

- スタブ新規作成: なし

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
プリミティブ型への依存を禁止する。以下の既存 DomainObject / ValueObject を利用する（新規作成なし）。

| クラス | パッケージ | 利用するアクセサ | 型 |
|---|---|---|---|
| `User` | `domain/model/` | `userId()`, `tenantId()`, `role()` | record コンポーネント |
| `UserId` | `domain/model/vo/` | `value()` | `Long` |
| `TenantId` | `domain/model/vo/` | `value()` | `Long` |
| `Role` | `domain/model/vo/` | `value()` | `String` |

- 作成する DomainObject / ValueObject 名: なし（既存利用のみ）

---

## 新規作成ファイル詳細

### 1. `JwtProperties`（record）
- パッケージ: `io.github.aki0057.multitenant.auth.infrastructure.security`
- アノテーション: `@ConfigurationProperties(prefix = "jwt")`
- フィールド（record コンポーネント）:
  - `String secret` — `application.properties` の `jwt.secret` にバインド
  - `Duration expiration` — `application.properties` の `jwt.expiration` にバインド（例: `15m`）
- コンストラクタバインド: record のため自動的にコンストラクタバインドが適用される（`@ConstructorBinding` 明示不要）

### 2. `JwtConfig`（@Configuration）
- パッケージ: `io.github.aki0057.multitenant.auth.infrastructure.security`
- アノテーション: `@Configuration`, `@EnableConfigurationProperties(JwtProperties.class)`
- Bean 定義: なし（`@EnableConfigurationProperties` だけで `JwtProperties` が Bean 登録される）

### 3. `JwtAccessTokenProvider`（@Component）
- パッケージ: `io.github.aki0057.multitenant.auth.infrastructure.security`
- アノテーション: `@Component`
- 依存: `JwtProperties`（コンストラクタインジェクション）
- `issue(User user)` 実装概要:
  - ライブラリ API: `io.jsonwebtoken.Jwts`（0.13.0）
  - キー生成: `io.jsonwebtoken.security.Keys.hmacShaKeyFor(...)` で `SecretKey` を生成
  - ペイロード設計:
    - `sub` = `String.valueOf(user.userId().value())`
    - `iat` = 発行時刻（`Date` / `Instant`）
    - `exp` = `iat + properties.expiration()`
    - カスタムクレーム `tenantId` = `user.tenantId().value()`（`Long`）
    - カスタムクレーム `role` = `user.role().value()`（`String`）
    - `iss` / `aud` / `jti` / `nbf` は付けない。`sub` の重複（`userId` クレーム）も付けない。
  - 署名: `Jwts.builder().signWith(key)`（jjwt がキー長から HS256 を自動選択）または明示的に `Jwts.SIG.HS256` を指定
  - 返却: `.compact()` の戻り値

### 4. 削除対象
- `StubAccessTokenProvider.java`（`infrastructure/security/` パッケージ）— `@Component` の Bean 重複を避けるため削除する

### 5. テストクラス
- クラス名: `JwtAccessTokenProviderTest`
- パッケージ: `io.github.aki0057.multitenant.auth.infrastructure.security`（テストソースツリー）
- テスト種別: プレーン JUnit 5（`@ExtendWith(MockitoExtension.class)` も不要）
- テストデータ: `JwtProperties` を直接インスタンス化（`new JwtProperties("test-secret-...", Duration.ofMinutes(15))`）
- 正常系:
  - `issue(user)` が非 null・非空の文字列を返すこと
  - 返却 JWT を jjwt でパースし、`sub`・`tenantId`・`role` の各クレームが期待値と一致すること
  - `exp` が `iat + expiration` の範囲内であること
- 異常系:
  - `user` が `null` の場合に `NullPointerException` がスローされること

---

## TODO.md との対応
以下のエントリが TODO.md に存在する。実装完了後にチェックを埋める。

- `infrastructure.security` セクション: `[ ] StubAccessTokenProvider を jjwt 実装へ置換`
- `Port (domain)` セクション: `[ ] AccessTokenProvider#issue`
