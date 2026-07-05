# tasklist

- [x] (条件: 作業対象が TODO.md に存在しない場合) 作業対象メソッド名（`AuthController#refresh`）を、プロジェクトルートの TODO.md の API (presentation) の章に、空のチェックボックスで追記する
- [x] 作業対象を実装する（`AuthController#refresh`、`RefreshRequest`、`RefreshResponse`）
- [x] `SecurityConfig` の `authorizeHttpRequests` の permitAll に `/refresh` を追加する（`.requestMatchers("/login", "/refresh").permitAll()` の 1 行のみの最小変更。TODO.md 登録は不要）
- [x] Javadoc を記載する
- [x] 正常系のテストコードを記載する
- [x] 異常系のテストコードを記載する
- [x] (条件: 作業対象が ValueObject でも Repository でもない場合) 内側レイヤーに作成したスタブのメソッド名（`AuthService#refresh(RefreshCommand)`）を、TODO.md の Service (application) の章に、空のチェックボックスで追記する
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックス（`AuthController#refresh`）を埋める（完了にする）
