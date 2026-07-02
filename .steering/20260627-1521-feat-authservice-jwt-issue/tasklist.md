# tasklist

- [x] `domain/service/AccessTokenProvider` インターフェース（スタブ・`// TODO` 目印付き）を `src/main/java/io/github/aki0057/multitenant/auth/domain/service/AccessTokenProvider.java` に新規作成する
- [x] `AuthService` に `AccessTokenProvider` フィールドを追加し（`@RequiredArgsConstructor` で DI）、`login` 末尾の `// TODO` / `return ""` を `return accessTokenProvider.issue(user);` に置き換える
- [x] `StubAccessTokenProvider`（`@Component`、`AccessTokenProvider` を implements、`issue(User user)` は `return "";`、`// TODO: jjwt 実装へ置換（後続 infrastructure 増分）` コメント付き）を `src/main/java/io/github/aki0057/multitenant/auth/infrastructure/security/StubAccessTokenProvider.java` に新規作成する
- [x] Javadoc を記載する（`AccessTokenProvider` インターフェースおよび `issue` メソッドに Javadoc を付与する。`AuthService#login` の既存 Javadoc は維持する）
- [x] 正常系のテストコードを記載する（`AuthServiceTest#login_success` に `AccessTokenProvider` の `@Mock` を追加し、`issue(user)` が任意のトークン文字列を返すよう stub した上で、`login` の戻り値がその文字列と一致することを `assertThat` で検証する）
- [x] 異常系のテストコードを記載する（`login_userNotFound` / `login_accountInactive` / `login_wrongPassword` の各ケースに `AccessTokenProvider` の `@Mock` を追加し、`BadCredentialsException` がスローされること・`issue` が呼ばれないこと（`verify(accessTokenProvider, never()).issue(any())`）を確認する）
- [x] `TODO.md` の `## Port (domain)` 章に `AccessTokenProvider#issue` が未登録の場合のみ空チェックボックスで追記する（既に登録済みであれば重複登録しない）
- [x] `TODO.md` に `## infrastructure.security` 章が存在しない場合は新設し、`StubAccessTokenProvider を jjwt 実装へ置換` を空チェックボックスで追記する
- [x] 今回実装した作業対象に対応する `TODO.md` の `Service (application)` 章の `- [ ] AuthService#login(LoginCommand) の JWT アクセストークン発行` チェックボックスを埋める（`[x]` にする）
