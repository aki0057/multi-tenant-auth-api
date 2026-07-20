# requirements

## 作業概要
`config/SecurityConfig` を修正し、`POST /auth/logout` を CSRF 保護有効チェーンへ載せ、認証不要（permitAll）とする。具体的には、`@Order(1)` の `refreshSecurityFilterChain` の `securityMatcher("/auth/refresh")` を `/auth/refresh` と `/auth/logout` の両方にマッチするよう変更する。当該チェーンは既に `anyRequest().permitAll()` かつ CSRF 有効・STATELESS のため、チェーン対象へ `/auth/logout` を含めれば permitAll と CSRF 保護有効の要件を満たす。ロジック分岐を持たない設定変更に限定し、既存 `/auth/refresh` の挙動は一切変更しない。

## 作業対象レイヤー
config（横断的関心事）

## 作業対象の種別
config

## 使用するテスト・フレームワーク等
- テストフレームワーク: なし（`docs/testing-guidelines.md` に従い config は単体テストを作らず、結合テストのフルコンテキスト起動で間接的に回帰担保する）。
- Spring MVC アノテーション: なし（`@Configuration` / `@EnableWebSecurity` の既存クラスへの設定変更）。使用する Spring Security API は `HttpSecurity#securityMatcher`（複数パターン指定）。

## 隣接レイヤー
- 1 つ外側のレイヤー: 該当なし（config のため。特定の業務レイヤーに属さない）
- 1 つ内側のレイヤー: 該当なし（config のため）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
該当なし（config のため）。DDD 契約項目（スタブ / Command / DomainObject / ValueObject の作成、外側／内側レイヤーとの契約）はいずれも発生しない。

## 作業対象メソッドのシグネチャ
既存メソッドの設定変更であり、新規メソッドのシグネチャ追加はない。
- 対象: `SecurityConfig#refreshSecurityFilterChain(HttpSecurity, CsrfTokenRepository)`（`@Order(1)`）
- 変更内容: `securityMatcher("/auth/refresh")` を `/auth/refresh` と `/auth/logout` の両方にマッチする指定へ変更する（例: `securityMatcher("/auth/refresh", "/auth/logout")`）。CSRF 有効・STATELESS・`anyRequest().permitAll()` の既存設定はそのまま流用する。
- Javadoc: 対象チェーンが `/auth/refresh` と `/auth/logout` を保護する旨へ更新する。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
該当なし（config のため）。内側スタブの作成・TODO.md 登録は発生しない。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
該当なし（config のため）。DomainObject / ValueObject の作成・利用は発生しない。
