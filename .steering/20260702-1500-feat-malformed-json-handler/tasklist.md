# tasklist

- [x] 作業対象を実装する（`GlobalExceptionHandler.java` に `handleMessageNotReadable(HttpMessageNotReadableException e)` を追加し、`HttpStatus.BAD_REQUEST` + `Map.of("error", "Malformed JSON request")` を返す。`org.springframework.http.converter.HttpMessageNotReadableException` の import を追加する。既存 3 ハンドラ（`handleBadCredentials` / `handleValidation` / `handleUnexpected`）と同じレスポンス形式に揃え、`handleValidation` の直後・`handleUnexpected` の直前に配置する）
- [x] Javadoc を記載する（既存ハンドラに倣い `/** 不正・壊れた JSON リクエストボディ（デシリアライズ失敗） → 400 Bad Request */` を記載する）
- [x] 正常系のテストコードを記載する（本タスクは異常系ハンドラの追加のため正常系テストは対象外。既存の正常系テスト（`GlobalExceptionHandlerTest` の既存3テスト、`LoginIntegrationTest#login_withValidCredentials_returns200`）が本改修後も green であることを確認する）
- [x] 異常系のテストコードを記載する
  - `GlobalExceptionHandlerTest` に `handleMessageNotReadable_returns400()` を追加する。`EXCEPTION_HOLDER.set(new HttpMessageNotReadableException("malformed json", mock(HttpInputMessage.class)))` を設定し `mockMvc.perform(get("/throw")).andExpect(status().isBadRequest())` を検証する。`HttpMessageNotReadableException(String)` は deprecated のため使用せず、`HttpInputMessage` を取るコンストラクタを使う。`org.springframework.http.HttpInputMessage` と `org.springframework.http.converter.HttpMessageNotReadableException` の import を追加する
  - `LoginIntegrationTest` に `login_withMalformedJson_returns400()` を追加する。`@DisplayName("異常系: 壊れた JSON ボディを送信すると 400 Bad Request が返る")` を付け、既存の `setUp()` フィクスチャを流用し、閉じ括弧を欠く等の構文的に不正な JSON ボディを POST /login し `.andExpect(status().isBadRequest())` を検証する
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックスを埋める（完了にする）（`GlobalExceptionHandler` の既存3ハンドラ（`handleBadCredentials` / `handleValidation` / `handleUnexpected`）は TODO.md に登録されていない慣例に倣い、`handleMessageNotReadable` も TODO.md に新規登録しない。対応するチェックボックスが存在しないことを確認し、TODO.md への変更は行わない）
