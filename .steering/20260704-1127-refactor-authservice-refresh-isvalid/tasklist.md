# tasklist

- [x] 作業対象（`AuthService#refresh` 内の判定 `oldToken.isRevoked() || oldToken.isExpired(now)` を `!oldToken.isValid(now)` へ置き換える）を実装する
- [x] Javadoc を記載する（`refresh` メソッドの Javadoc に実装詳細の変更に伴う不整合があれば更新する。`@throws` 等の契約は変更しない）
- [x] 正常系のテストコードを記載する（既存 `AuthServiceTest#refresh_success` を実行し、戻り値・`refreshTokenRepository.save` の呼び出し回数と引数内容が従来どおり green であることを確認する。挙動に差異があれば挙動を変えずに追随修正する）
- [x] 異常系のテストコードを記載する（既存 `AuthServiceTest` の `refresh_tokenNotFound` / `refresh_tokenRevoked` / `refresh_tokenExpired` / `refresh_userNotFound` / `refresh_userInactive` を実行し、いずれも green であることを確認する。挙動に差異があれば挙動を変えずに追随修正する）
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックスを確認する（`## Service (application)` 章の `AuthService#refresh(RefreshCommand)` は既に `- [x]` 済みのため、本増分では変更不要であることを確認する）
