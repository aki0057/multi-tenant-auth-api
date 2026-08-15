# tasklist

- [x] 作業対象を実装する（`src/test/java/io/github/aki0057/multitenant/auth/ArchitectureTest.java` を作成し、要件の 5 ルールを ArchUnit で記述する。`..config..` と `MultiTenantAuthApiApplication` を検査対象から除外する）
- [x] Javadoc を記載する（テストクラスおよび各ルールの意図を説明する）
- [x] 正常系のテストコードを記載する（現状の依存構造でルールが green になることを、5 つの依存ルールとして表現する）
- [x] 異常系のテストコードを記載する（違反があれば failure となるルール記述であること。ArchUnit の宣言的ルール自体が違反検出＝異常系を担うため、禁止依存を表す `noClasses()...should().dependOnClassesThat()` 形式で明示する）
