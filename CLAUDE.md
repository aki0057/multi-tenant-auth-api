# CLAUDE.md

## 概要

Spring Boot を用いたマルチテナント対応の認証認可 API プロジェクト。

---

## ドキュメント

| ファイル                           | 内容                                     |
|--------------------------------|----------------------------------------|
| `docs/repository-structure.md` | ディレクトリ構成・ファイル配置ルール                     |
| `docs/database-design.md`      | ER 図・テーブル定義・共通カラム方針                    |
| `docs/file-change-workflow.md` | ファイル変更手順・DDD アーキテクチャ・スタブ / Command の扱い |
| `docs/testing-guidelines.md`   | レイヤー別テスト方針・命名規則・テストデータ管理               |
| `docs/git-conventions.md`      | コミットメッセージ規約（type・prefix）               |

---

## Commands

```bash
# ローカルDB起動（要 .env ファイル）
docker compose up -d

# 特定クラスのテストを実行（FooTest は実際のテストクラス名に置き換える）
./mvnw test -Dtest=<FooTest>

# テストのみ
./mvnw clean test

# カバレッジレポート生成（target/site/jacoco/index.html）
./mvnw clean verify

# ビルド
./mvnw clean install
```

テスト実行にローカル DB は不要。テストプロファイル（`@ActiveProfiles("test")`）では H2 インメモリ DB を使用する。

---

## 重要制約

- 当プロジェクトの**全て**のファイルは、ユーザーの許可なく変更してはならない。
- `src/`配下のファイルを変更するすべての作業は、必ず `docs/file-change-workflow.md` のパイプラインに従う。メインセッションが planner → plan-verifier →【ユーザー承認】→ implementer → tester → done-auditor の 5 段階を順に駆動する。
