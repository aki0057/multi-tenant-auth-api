# tasklist

- [x] 作業対象を実装する（`AuthService.logout(LogoutCommand)` の本体。`refreshToken` の `null` / VO 検証失敗 / トークン不存在はいずれも例外を投げず `void` 復帰。見つかった場合は失効済み・期限切れを問わず無条件に `revoke()` → `save()`）
- [x] Javadoc を記載する（冪等・例外を投げない契約、失効方式、単一トークン失効であることを明記。既存スタブの Javadoc を整える）
- [x] 正常系のテストコードを記載する（有効トークン提示 → `revoke()` された `RefreshToken` が `save` されることを検証）
- [x] 異常系のテストコードを記載する（`refreshToken` が `null` / 形式不正（空白）/ 不存在 / 失効済み / 期限切れ の各ケースで例外を投げず、かつ不存在・null・形式不正では `save` を呼ばないこと／失効済み・期限切れでは `revoke`→`save` されることを検証）
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックスを埋める（`## Service (application)` の `AuthService#logout(LogoutCommand)` を完了にする）
