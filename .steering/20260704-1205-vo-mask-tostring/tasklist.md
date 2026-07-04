# tasklist

## 前提確認・TODO.md 準備

- [x] `RawRefreshToken#toString` が TODO.md の ValueObject (domain) 章に存在しないため、空のチェックボックスで追記する
- [x] `RawPassword#toString` が TODO.md の ValueObject (domain) 章に存在しないため、空のチェックボックスで追記する

## RawRefreshToken への toString() オーバーライド

- [x] `domain/model/vo/RawRefreshToken.java` に `toString()` をオーバーライドし、値を含まないマスキング済み固定文字列（例: `"RawRefreshToken[masked]"`）を返すよう実装する
- [x] `toString()` に Javadoc を記載する（機密値をログ等へ流出させないためのマスキングである旨を明記する）
- [x] `RawRefreshTokenTest.java` に正常系テストを追記する（ユーザー指示により、`toString()` の戻り値が生成に使った元の `String` と等しくないことのみを検証する。異常系テストは対象外とする。理由は requirements.md の「作業概要」を参照）

## RawPassword への toString() オーバーライド

- [x] `domain/model/vo/RawPassword.java` に `toString()` をオーバーライドし、値を含まないマスキング済み固定文字列（例: `"RawPassword[masked]"`）を返すよう実装する
- [x] `toString()` に Javadoc を記載する（機密値をログ等へ流出させないためのマスキングである旨を明記する）
- [x] `RawPasswordTest.java` に正常系テストを追記する（ユーザー指示により、`toString()` の戻り値が生成に使った元の `String` と等しくないことのみを検証する。異常系テストは対象外とする。理由は requirements.md の「作業概要」を参照）

## TODO.md の最終更新

- [x] `RawRefreshToken#toString` の TODO.md（ValueObject (domain) 章）のチェックボックスを埋める（完了にする）
- [x] `RawPassword#toString` の TODO.md（ValueObject (domain) 章）のチェックボックスを埋める（完了にする）
