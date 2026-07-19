# requirements

## 作業概要
ArchUnit（archunit-junit5 1.4.1、pom.xml へ追加済み）を用いて、本プロジェクトの DDD レイヤードアーキテクチャの依存方向を静的に検査するアーキテクチャテストクラスを 1 つ作成する。成果物はテストクラスのみで、`src/main/` の業務コードは一切変更しない。

ベースパッケージ: `io.github.aki0057.multitenant.auth`
テストクラス: `src/test/java/io/github/aki0057/multitenant/auth/ArchitectureTest.java`

検査するレイヤーは「緩いレイヤード」（外側は内側すべてのレイヤーに依存可）で表現する。注意点として、`docs/file-change-workflow.md` の図は**実行時の呼び出し方向**であり、ソースコードのコンパイル時依存は DIP により `infrastructure → domain` となる（infrastructure が domain の Repository インターフェースを実装するため）。したがってソース依存ルールは以下の 5 点で検査する。

1. domain は他レイヤー（presentation / application / infrastructure / config）に依存しない。
2. domain はフレームワーク（`org.springframework..`、`jakarta..`）に依存しない。
3. application は presentation / infrastructure / config に依存しない（domain への依存のみ許可）。
4. presentation は infrastructure / config に依存しない（application / domain への依存は許可）。
5. infrastructure は presentation / application / config に依存しない（domain への依存のみ許可＝DIP の検査）。

除外対象: `..config..` パッケージ全体と、ルート直下の `MultiTenantAuthApiApplication` はルール検査の対象から外す（config は横断的関心事であり、複数レイヤーへ依存してよいため。実測でも config は presentation.filter / domain.service に依存する）。

実測済みの現状依存（すべて上記ルールで green になることを確認済み）:
- presentation → application・domain
- application → domain
- domain → domain のみ（フレームワーク import ゼロ）
- infrastructure → domain
- config → presentation.filter・domain.service（config は除外対象）

## 作業対象レイヤー
config（横断的関心事 / アーキテクチャテスト。特定の業務レイヤーに属さない）

## 作業対象の種別
config（横断的関心事）

## 使用するテスト・フレームワーク等
- テストフレームワーク: JUnit 5 + ArchUnit（`com.tngtech.archunit:archunit-junit5:1.4.1`）
- Spring MVC アノテーション: 使用しない（Spring コンテキスト不要・DB 不要の純粋な静的解析テスト）
- 補足: `@AnalyzeClasses(packages = "io.github.aki0057.multitenant.auth")` と `@ArchTest` フィールド、または `ClassFileImporter` + `layeredArchitecture()` / `noClasses()` ルールで実装する想定。

## 隣接レイヤー
- 1 つ外側のレイヤー: 該当なし（config のため。アーキテクチャテストはどの業務レイヤーにも属さず、外側／内側の概念を持たない）
- 1 つ内側のレイヤー: 該当なし（config のため）

## 外側レイヤーとの契約
該当なし（config のため）。アーキテクチャテストは既存の業務コードのシグネチャに依存せず、パッケージ構造のみを検査する。

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一の成果物（テストクラス）。
- クラス名: `ArchitectureTest`
- 内容: 上記 5 ルールを検査する ArchUnit ルール（`@ArchTest` フィールド群、または `@Test` メソッド群）
- 引数 / 戻り値: 該当なし（テストメソッドは引数なし・戻り値 void）

## 内側レイヤーへの契約
該当なし（config のため）。スタブ / Command の作成は発生しない。

## ドメインモデルの利用
該当なし（config のため）。DomainObject / ValueObject の作成・利用は発生しない。
