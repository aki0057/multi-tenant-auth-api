# requirements

## 作業概要
既存の認証エンドポイントのパスを `/auth` 配下へ移設する。具体的には `POST /login` → `POST /auth/login`、`POST /refresh` → `POST /auth/refresh` へ変更する。あわせて refreshToken クッキーの `Path` 属性（`/refresh` → `/auth/refresh`）と、CSRF 保護用 SecurityFilterChain の `securityMatcher`（`/refresh` → `/auth/refresh`）、認証不要設定の `requestMatchers`（`/login` → `/auth/login`）を連動させる。業務ロジック（認証・トークン発行・ローテーション処理）は一切変更しない。既存パス文字列の付け替えとドキュメント（Javadoc / OpenAPI / README）更新のみを行う。

## 作業対象レイヤー
config（横断的関心事）。ルーティング設定の変更であり DDD の単一業務レイヤーに属さないため、config の単一レイヤー原則の例外として presentation(Controller) と config(SecurityConfig / OpenApiConfig) の既存ファイルを横断してパス文字列とドキュメントを更新する。ロジック変更は伴わない。

## 作業対象の種別
config（横断的関心事）

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit（Spring MVC MockMvc による既存結合テスト）。単体テストは新設せず、既存の `AuthControllerTest` / `LoginIntegrationTest` のパス・クッキー Path 検証を新パスへ追随させて回帰担保する。
- Spring MVC アノテーション: `@PostMapping`（値の文字列変更のみ。新規アノテーション追加なし）

## 隣接レイヤー
- 1 つ外側のレイヤー: なし（config のため、外部クライアントからのルーティング設定。特定の業務レイヤーを外側に持たない）
- 1 つ内側のレイヤー: なし（config のため、内側の業務レイヤーを呼び出さない）

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
該当なし（config のため）。業務レイヤー間の契約は発生しない。

## 作業対象メソッドのシグネチャ
config のためメソッドの新規追加・シグネチャ変更はない。既存メソッドの本体ロジックも不変。変更するのは以下の既存コード上のパス文字列とドキュメントのみ。
- `presentation/AuthController.java`: `@PostMapping("/login")`(63行) → `"/auth/login"`、`@PostMapping("/refresh")`(107行) → `"/auth/refresh"`、`buildRefreshTokenCookie` の `.path("/refresh")`(131行) → `.path("/auth/refresh")`、Javadoc / `@Operation` / `@Parameter` 記述内のパス表記。`login` / `refresh` メソッド自身のシグネチャ（引数・戻り値）は不変。
- `config/SecurityConfig.java`: `.securityMatcher("/refresh")`(61行) → `"/auth/refresh"`、`.requestMatchers("/login").permitAll()`(122行) → `"/auth/login"`、Javadoc 内のパス表記。Bean メソッドのシグネチャは不変。
- `config/OpenApiConfig.java`: Javadoc 内のパス表記のみ。
- 補足: refreshToken クッキー Path と CSRF 用 `securityMatcher` は `/auth/refresh` へ必ず連動させる（不整合だと refresh 時に Cookie 未送信 / CSRF チェーン不一致となる）。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
該当なし（config のため）。スタブ / Command の作成、内側レイヤーの呼び出しは発生しない。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
該当なし（config のため）。DomainObject / ValueObject の作成・利用は発生しない。
