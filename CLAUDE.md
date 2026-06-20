# CLAUDE.md

## 概要

Spring Boot を用いたマルチテナント対応の認証認可 API プロジェクト。

---

## ドキュメント

| ファイル                             | 内容                    |
|----------------------------------|-----------------------|
| `docs/architecture.md`           | マルチテナント方式・ドメインモデル設計方針 |
| `docs/repository-structure.md`   | ディレクトリ構成・ファイル配置ルール    |
| `docs/database-design.md`        | ER 図・テーブル定義・共通カラム方針   |
| `docs/development-guidelines.md` | コーディング規約・ドキュメント管理ルール  |

---

## Commands

```bash
# ローカルDB起動（要 .env ファイル）
docker compose up -d

# ビルド（テスト含む）
./mvnw clean install

# テストのみ
./mvnw clean test

# 特定クラスのテストを実行
./mvnw test -Dtest=LoginIntegrationTest

# カバレッジレポート生成（target/site/jacoco/index.html）
./mvnw verify
```

テスト実行にローカル DB は不要。テストプロファイル（`@ActiveProfiles("test")`）では H2 インメモリ DB を使用する。

---

## 重要制約

- `accessDeniedHandler` は 403 ではなく **404 を返す**（リソース存在を隠蔽するセキュリティ要件）
- `JwtAuthenticationFilter` は現在 **曳光弾用のスタブ**。全リクエストを `dev-user` として認証通過させている。`JwtService` 実装後にダミー認証を削除する
- セッションは STATELESS、CSRF 無効

---

## 作業開始プロセス

新しい作業を開始する前に、必ず以下の手順を実施すること。

### 1. ステアリングディレクトリの作成

```bash
mkdir -p .steering/[YYYYMMDD]-[作業内容]
```

**命名規則：**

```
.steering/[YYYYMMDD]-[作業内容]/
```

例：
- `.steering/20240101-feat-login-endpoint/`
- `.steering/20240215-fix-filer-bug/`
- `.steering/20240310-refactor-auth-service/`

### 2. 作業ドキュメントの作成

以下のファイルを作成し、内容を記述する。

- `.steering/[YYYYMMDD]-[作業内容]/requirements.md` — 要件の定義
- `.steering/[YYYYMMDD]-[作業内容]/tasklist.md` — 作業内容の定義
