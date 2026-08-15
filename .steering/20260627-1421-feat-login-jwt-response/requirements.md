# requirements

## 作業概要

`POST /login` のレスポンスを `ResponseEntity<Void>`（ボディなし）から `ResponseEntity<LoginResponse>` に変更し、JWT アクセストークンを JSON で返す。新規レスポンス DTO `LoginResponse` を `presentation/` 直下に作成する。内側の `AuthService#login` のシグネチャを `void` から `String` に変更し、既存の認証検証ロジックは保持したまま JWT トークン発行部分のみを `// TODO` として後続増分に委ねる。

## 作業対象レイヤー

presentation

## 作業対象の種別

API

## 使用するテスト・フレームワーク等

- テストフレームワーク: JUnit 5 / Mockito
- Spring MVC アノテーション: @WebMvcTest, MockMvc

## 隣接レイヤー

- 1 つ外側のレイヤー: なし（presentation は最外層）
- 1 つ内側のレイヤー: application (AuthService)

## 作業対象メソッドのシグネチャ

作業概要を達成するための単一メソッド。

- メソッド名: login
- 引数: `@Valid @RequestBody LoginRequest request`
- 戻り値: `ResponseEntity<LoginResponse>`

新規作成するレスポンス DTO:

```java
// presentation/LoginResponse.java
public record LoginResponse(String accessToken, String tokenType) {}
```

`LoginRequest`・`LoginCommand` は変更しない。

## 内側レイヤーへの契約

**変則ケース: AuthService#login は既存実装済みのメソッドであり、通常スタブ（未存在の新規作成）とは異なる扱いをする。**

通常本ワークフローは「内側はまだ存在しない → 新規スタブ作成」を前提とするが、本件では `AuthService#login` がすでに認証検証ロジックを持ち `void` を返している。既存の検証ロジック（`TenantCode`/`Email` による `UserRepository` 検索・パスワード照合・アカウント有効性確認）は一切破棄しない。

変更内容は次の 2 点のみ:

1. シグネチャの戻り値を `void` → `String` に変更する。
2. メソッド末尾にトークン発行の `// TODO` を追加し、`return` は仮の空文字列 `return "";` としてコンパイルを通す。

- 変更前シグネチャ: `public void login(LoginCommand command)`
- 変更後シグネチャ: `public String login(LoginCommand command)`
- 引数: `LoginCommand command`（`LoginCommand` は既存・完成扱いのため変更不要）
- TODO 登録: `AuthService#login` は TODO.md の Service 章でチェック済み（[x]）。本増分では JWT トークン発行部分のみ新規 TODO として Service 章に追記する。
