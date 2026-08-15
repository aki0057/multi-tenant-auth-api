# tasklist

Pass 1（application 単独）。上から順に実施する。`(条件: ...)` が付いた項目は条件に該当する場合のみ実施する。
Pass 1 末尾でプロジェクト全体が green（既存 login 経路は不変・新メソッドは追加のみ）であることを必須とする。
Pass 1 では `AuthController` / `LoginResponse` / 既存 `String login(LoginCommand)` シムを一切変更しない。

- [x] (条件: 作業対象が TODO.md に存在しない場合) 作業対象メソッド名を TODO.md の Service (application) 章に空のチェックボックスで追記する
      ※ 一時名 `loginWithRefreshToken` は Pass 2 で `login` に統合され消滅するため、Pass 1 では TODO.md に登録しない（requirements.md「TODO.md の扱い」参照）。本項目はスキップ。
- [x] 作業対象を実装する（application 層に `record LoginResult(String accessToken, String refreshToken)` を新設し、`AuthService` に一時名メソッド `loginWithRefreshToken(@NonNull LoginCommand) -> LoginResult` を `@Transactional` で追加。認証は既存 login と同一、認証成功後にリフレッシュトークンを新規発行・ハッシュ化・`refreshTokenRepository.save`。既存 `String login(LoginCommand)` シムは変更しない）
- [x] Javadoc を記載する（`loginWithRefreshToken` に、リフレッシュトークン新規発行・`@Transactional`・`BadCredentialsException` への一律変換・一時名である旨を記載。`LoginResult` にも記載）
- [x] 正常系のテストコードを記載する（`AuthServiceTest` に追記: 認証成功時にアクセストークンとリフレッシュトークンが `LoginResult` に載ること、リフレッシュトークンが生成・ハッシュ化され `refreshTokenRepository.save` が呼ばれること）
- [x] 異常系のテストコードを記載する（`AuthServiceTest` に追記: VO 検証失敗 / ユーザー不在 / パスワード不一致 で `BadCredentialsException`）
- [x] (条件: 作業対象が ValueObject でも Repository でもない場合) 内側レイヤーに作成したスタブのメソッド名を TODO.md の該当章に追記する
      ※ 本作業で新規作成する内側スタブは無い（全依存が refresh 実装時に作成・実装済み）。追記対象なし。
- [x] (条件: DomainObject / ValueObject を作成した場合) 作成した DomainObject 名を TODO.md の該当章に追記する
      ※ 本作業で新規作成する DomainObject / ValueObject は無い。追記対象なし。
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックスを埋める（完了にする）
      ※ Pass 1 では TODO.md に登録も更新も行わない（TODO.md の整合は Pass 2 で実施）。本項目はスキップ。
