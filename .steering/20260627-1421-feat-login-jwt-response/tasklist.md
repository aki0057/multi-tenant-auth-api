# tasklist

- [x] `AuthController#login` を TODO.md の API (presentation) 章に空のチェックボックスで追記する
- [x] `presentation/LoginResponse.java` を新規作成する（`public record LoginResponse(String accessToken, String tokenType) {}`）
- [x] `AuthService#login` のシグネチャを `void` から `String` に変更する。既存の認証検証ロジックは保持し、メソッド末尾にトークン発行の `// TODO` と仮の `return "";` を追加してコンパイルを通す
- [x] `AuthController#login` を `ResponseEntity<LoginResponse>` を返すよう実装する（`authService.login(command)` の戻り値 `accessToken` を受け取り、`new LoginResponse(accessToken, "Bearer")` を 200 で返す）
- [x] Javadoc を記載する（`AuthController#login`・`LoginResponse`）
- [x] 正常系のテストコードを記載する（MockMvc で 200 かつ `{"accessToken":"<モック値>","tokenType":"Bearer"}` を検証。AuthService を Mock して任意のトークン文字列を返させる）
- [x] 異常系のテストコードを記載する（`BadCredentialsException` → 401、バリデーションエラー → 400）
- [x] `AuthService#login` の JWT トークン発行（`// TODO` 部分）を TODO.md の Service (application) 章に空のチェックボックスで追記する
- [x] 今回実装した `AuthController#login` に対応する TODO.md のチェックボックスを埋める（完了にする）
