# tasklist

- [x] `src/test/java/io/github/aki0057/multitenant/auth/infrastructure/security/PasswordEncoderVerifierTest.java` を作成し、Spring 未起動・コンストラクタ直接インスタンス化（`new PasswordEncoderVerifier(new BCryptPasswordEncoder())`）でテストクラスの骨組みを用意する
- [x] テストクラスに Javadoc を記載する（対象クラス・技術アダプターとして Spring コンテキストを起動しない旨を明記）
- [x] 正常系のテストコードを記載する（生パスワードと一致する BCrypt ハッシュを照合すると `matches` が `true` を返すことを検証する）
- [x] 異常系のテストコードを記載する（生パスワードと一致しない BCrypt ハッシュを照合すると `matches` が `false` を返すことを検証する）
