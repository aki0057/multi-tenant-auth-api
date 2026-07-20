# tasklist

- [x] (条件: 作業対象が TODO.md に存在しない場合) `AuthController#logout` を、プロジェクトルートの TODO.md の「API (presentation)」の章に、空のチェックボックスで追記する
- [x] 作業対象を実装する（`AuthController#logout` の実装、既存 `AuthController#buildRefreshTokenCookie` の Path を `/auth` へ変更、内側 `AuthService#logout` スタブと `LogoutCommand` record の作成）
- [x] Javadoc を記載する
- [x] 正常系のテストコードを記載する
- [x] 異常系のテストコードを記載する
- [x] (条件: 作業対象が ValueObject でも Repository でもない場合) 内側レイヤーに作成したスタブ `AuthService#logout(LogoutCommand)` を、TODO.md の「Service (application)」の章に、空のチェックボックスで追記する
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックス（`AuthController#logout`）を埋める（完了にする）
