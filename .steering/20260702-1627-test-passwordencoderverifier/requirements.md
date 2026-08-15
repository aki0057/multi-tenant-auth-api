# requirements

## 作業概要
`infrastructure.security.PasswordEncoderVerifier`（`src/main/java/io/github/aki0057/multitenant/auth/infrastructure/security/PasswordEncoderVerifier.java`）の単体テストを実装する。

**注記（本タスクの特記事項）**: 本体実装（`PasswordEncoderVerifier` 本体、`domain.service.PasswordVerifier` インターフェース、`RawPassword` / `PasswordHash` VO）は既に完成・コミット済みであり、`TODO.md` の `infrastructure.security` 章でも `PasswordEncoderVerifier` は完了チェック済みである。本タスクで追加するのは **テストコードのみ**。本体・VO・インターフェースへの変更は一切行わない。

## 作業対象レイヤー
infrastructure(security)

## 作業対象の種別
infrastructure.security

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 (Jupiter) + AssertJ（`assertThat`）。Mockito は使用しない。
- Spring MVC アノテーション: なし（`testing-guidelines.md` の「技術アダプター（ドメインポート実装）」方針に従い、Spring コンテキストを起動せず、`PasswordEncoderVerifier` をコンストラクタで直接インスタンス化する）。
- `PasswordEncoder` の実体には、実際に使用される `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`（`config/PasswordEncoderConfig` が Bean 化しているものと同一の実装）をテストメソッド内で `new` して用いる（モック不要）。

## 隣接レイヤー
- 1 つ外側のレイヤー: domain（`domain.service.PasswordVerifier` インターフェース／`domain.model.vo.RawPassword` / `PasswordHash`）
- 1 つ内側のレイヤー: Spring Security ライブラリの `PasswordEncoder`（`BCryptPasswordEncoder`）。既存の実在ライブラリクラスであり、本タスクでの新規作成・変更はない。

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる（今回はテスト追加のみのため、シグネチャの変更はしない）。
- メソッド名: `PasswordVerifier#matches`
- 引数: `RawPassword rawPassword`, `PasswordHash passwordHash`
- 戻り値: `boolean`

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド（既存実装。変更しない）。
- メソッド名: `PasswordEncoderVerifier#matches`
- 引数: `RawPassword rawPassword`, `PasswordHash passwordHash`
- 戻り値: `boolean`（`passwordEncoder.matches(rawPassword.value(), passwordHash.value())` の結果をそのまま返す）

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
内側（`PasswordEncoder` / `BCryptPasswordEncoder`）は Spring Security が提供する実在クラスであり、既に存在する。新規スタブの作成は不要。テストでは `new BCryptPasswordEncoder()` を用いて、`encode()` で生成した実ハッシュ値を `PasswordHash` に渡して照合する。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
プリミティブ型への依存を禁止する。テストコードにおいても、`matches` の引数は既存の VO である `RawPassword` / `PasswordHash` をテストメソッド内で直接生成して使用する（`testing-guidelines.md` のテストデータ管理方針に従う）。新規の DomainObject / ValueObject の作成はない。
- 作成する DomainObject / ValueObject 名: なし（既存の `RawPassword` / `PasswordHash` を利用）
