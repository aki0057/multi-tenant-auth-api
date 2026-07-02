# tasklist

- [x] 作業対象（`RawPassword` のコンパクトコンストラクタ）を実装する（`null` 拒否・空白拒否・8 文字以上・半角英数字のみを検証し、違反時は `IllegalArgumentException` をスローする）
- [x] Javadoc を記載する（クラス Javadoc とコンパクトコンストラクタの `@param` / `@throws` を、既存の `Email` / `Role` VO の記載パターンに合わせて記載する）
- [x] 正常系のテストコードを記載する（`RawPasswordTest` を新規作成し、有効な値で `value()` が同じ値を返すこと、8 文字ちょうどの境界値などを検証する）
- [x] 異常系のテストコードを記載する（`null`・空文字・空白のみ・8 文字未満・半角英数字以外の文字（記号・全角・空白混在等）を渡した場合に `IllegalArgumentException` がスローされることを検証する）
- [x] 今回実装した作業対象に対応する TODO.md のチェックボックスを埋める（完了にする）
