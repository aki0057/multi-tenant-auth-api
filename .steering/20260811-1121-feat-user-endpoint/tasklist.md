# tasklist

- [x] (条件: 作業対象が TODO.md に存在しない場合) `UserController#getMe` を、プロジェクトルートの TODO.md の「API (presentation)」の章に、空のチェックボックスで追記する
- [x] 作業対象を実装する（`presentation/UserController#getMe` と `presentation/UserResponse` の新規作成、内側の `application/UserService#getMe` スタブ（`// TODO` のみ・中身を実装しない）と `application/GetMeCommand` / `application/GetMeResult` record の作成。`config/SecurityConfig`・`AuthenticatedUser`・`GlobalExceptionHandler` は変更しない）
- [x] Javadoc を記載する
- [x] 正常系のテストコードを記載する（認証済み principal で `GET /users/me` → 200 OK・`email` / `role` が返る）
- [x] 異常系のテストコードを記載する（該当ユーザーが存在しない（`Optional.empty()`）→ 404 Not Found／未認証（principal なし）→ 401 Unauthorized）
- [x] (条件: 作業対象が ValueObject でも Repository でもない場合) 内側レイヤーに作成したスタブ `UserService#getMe(GetMeCommand)` を、TODO.md の「Service (application)」の章に、空のチェックボックスで追記する
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックス（`UserController#getMe`）を埋める（完了にする）
