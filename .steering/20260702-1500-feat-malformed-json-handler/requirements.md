# requirements

## 作業概要

不正・壊れた JSON リクエストボディ（デシリアライズ失敗）を送信した場合、Spring MVC は `HttpMessageNotReadableException` を投げる。現状 `GlobalExceptionHandler` にはこれを個別に処理するハンドラが無いため、`@ExceptionHandler(Exception.class)`（`handleUnexpected`）にフォールバックし HTTP 500 Internal Server Error が返ってしまう。

本タスクでは `GlobalExceptionHandler` に `HttpMessageNotReadableException` 専用の `@ExceptionHandler` メソッドを追加し、既存の `handleValidation`（`MethodArgumentNotValidException` → 400）と同様に HTTP 400 Bad Request を返すよう改修する。レスポンス形式は既存 3 ハンドラと同じ `Map.of("error", <message>)` の JSON に揃える。

本タスクは presentation 層（`presentation/advice/`）に閉じた変更であり、`GlobalExceptionHandler` は application 層（Service）を呼び出さない自己完結的なコンポーネントである。新規の内側呼び出し・新規スタブ・新規 DomainObject/ValueObject の作成は発生しない。

## 作業対象レイヤー

presentation（例外ハンドラー: `presentation/advice/GlobalExceptionHandler`）

## 作業対象の種別

API

補足: `GlobalExceptionHandler` は `@RestControllerAdvice` であり、`docs/repository-structure.md` のファイル配置ルール上は「例外ハンドラー」という独立した種別として扱われる。`docs/file-change-workflow.md` の判定早見表には「例外ハンドラー」の行が無いため、presentation 層で最も近い「API」に区分する。ただし既存 3 ハンドラ（`handleBadCredentials` / `handleValidation` / `handleUnexpected`）と同様、本ハンドラも application 層を呼び出さず、例外を HTTP レスポンスへ変換するだけの自己完結的な処理である点に留意する。

## 使用するテスト・フレームワーク等

- テストフレームワーク:
  - `GlobalExceptionHandlerTest`: JUnit5 + `MockMvcBuilders.standaloneSetup()`（Spring コンテキスト未起動）。既存の `EXCEPTION_HOLDER`（`AtomicReference<Exception>`）+ `DummyController` パターンをそのまま踏襲する。例外インスタンス生成に Mockito の `mock(HttpInputMessage.class)` を使用する（`org.mockito.Mockito.mock` を static import 済み）。
  - `LoginIntegrationTest`: JUnit5 + `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` + `@Transactional`（既存の `login_withValidCredentials_returns200` / `login_withInvalidFormatCredentials_returns401` と同じクラス・同じ構成に 1 テストケースを追加する）。
- Spring MVC アノテーション: `@ExceptionHandler`（`GlobalExceptionHandler` に追加するアノテーション）。新規の `@RestControllerAdvice` は不要（既存クラスへの追記）。

## 隣接レイヤー

- 1 つ外側のレイヤー: なし相当（`@RestControllerAdvice` は Spring MVC の `DispatcherServlet` が例外発生時に自動的に呼び出す仕組みであり、業務コードから明示的に呼び出されるメソッドではない。既存 3 ハンドラと同様、外側レイヤーとの明示的な呼び出し契約は存在しない）。
- 1 つ内側のレイヤー: なし（本ハンドラは `Map.of("error", ...)` を組み立てて `ResponseEntity` を返すのみで、application 層以下を呼び出さない）。

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)

該当なし（作業対象は API のため本節は不要）。

## 作業対象メソッドのシグネチャ

作業概要を達成するための単一メソッド。`GlobalExceptionHandler` に新規メソッドとして追加する（既存の `handleBadCredentials` / `handleValidation` / `handleUnexpected` と同じクラス内に並べる）。

- メソッド名: `handleMessageNotReadable`
- 引数: `HttpMessageNotReadableException e`（`org.springframework.http.converter.HttpMessageNotReadableException`）
- 戻り値: `ResponseEntity<Map<String, String>>`

```java
/**
 * 不正・壊れた JSON リクエストボディ（デシリアライズ失敗） → 400 Bad Request
 */
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<Map<String, String>> handleMessageNotReadable(HttpMessageNotReadableException e) {
    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "Malformed JSON request"));
}
```

- エラーメッセージ文言: `"Malformed JSON request"`（既存 3 ハンドラの `"Invalid credentials"` / `"Validation failed"` / `"Internal server error"` に倣った簡潔な英語表現。実装者はこの文言をそのまま使用する）。
- クラス内の配置順: `handleValidation`（400 系の既存ハンドラ）の直後に追加し、`handleUnexpected`（`Exception.class`、最も広い型）より前に置く（`@ExceptionHandler` の解決順序に影響はないが、可読性のため 400 系ハンドラをまとめる）。
- import 追加: `org.springframework.http.converter.HttpMessageNotReadableException` を `GlobalExceptionHandler.java` に追加する。

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)

該当なし。本タスクは新規の内側呼び出し・新規スタブを一切作成しない。`handleMessageNotReadable` は `Map.of(...)` を用いてレスポンスを組み立てるのみで、application 層以下（Service / Repository / infrastructure）を呼び出さない。既存の `handleValidation` / `handleUnexpected` と同じ自己完結パターンに倣う。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)

該当なし（作業対象は API のため本節は不要）。

## テスト追加箇所の詳細

### 1. `GlobalExceptionHandlerTest`（ユニットテスト）

`src/test/java/io/github/aki0057/multitenant/auth/presentation/advice/GlobalExceptionHandlerTest.java` に、既存 3 テストと同じ形式で 1 テストメソッドを追加する。

- テストメソッド名: `handleMessageNotReadable_returns400()`
- 手順: `EXCEPTION_HOLDER.set(new HttpMessageNotReadableException("malformed json", mock(HttpInputMessage.class)))` を設定し、`mockMvc.perform(get("/throw")).andExpect(status().isBadRequest())` を検証する。
- 使用するコンストラクタ: `HttpMessageNotReadableException(String msg, HttpInputMessage httpInputMessage)`（`org.springframework.http.HttpInputMessage`）。`HttpMessageNotReadableException(String msg)` は Spring 6.2 系で `@Deprecated` のため使用しない。`HttpInputMessage` はインターフェースのため `org.mockito.Mockito.mock(HttpInputMessage.class)`（クラス内で既に static import 済みの `mock`）でモックする。
- 追加 import: `org.springframework.http.HttpInputMessage`、`org.springframework.http.converter.HttpMessageNotReadableException`。

### 2. `LoginIntegrationTest`（結合テスト）

`src/test/java/io/github/aki0057/multitenant/auth/LoginIntegrationTest.java` に、既存の `login_withValidCredentials_returns200` / `login_withInvalidFormatCredentials_returns401` と同じ構成（`mockMvc.perform(post("/login")...)`)で 1 テストケースを追加する。

- テストメソッド名: `login_withMalformedJson_returns400()`
- `@DisplayName`: `"異常系: 壊れた JSON ボディを送信すると 400 Bad Request が返る"`
- リクエストボディ: 構文的に壊れた JSON（例: 閉じ括弧の欠落や末尾カンマなど、Jackson がパースできない文字列）を `.content(...)` に渡す。例:
  ```java
  .content("""
          {
            "tenantCode": "testTenant",
            "email": "test@example.com",
            "password": "password"
          """)
  ```
  （末尾の `}` を欠落させ、JSON として不正な状態にする）
- 検証: `.andExpect(status().isBadRequest())`
- 新規 `@BeforeEach` のデータ投入は不要（既存の `setUp()` のテナント／ユーザー登録をそのまま利用してよい。壊れた JSON はデシリアライズ段階で失敗するため DB アクセスに到達しない）。
