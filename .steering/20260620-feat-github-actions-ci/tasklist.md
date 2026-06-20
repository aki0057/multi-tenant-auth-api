# タスクリスト: GitHub Actions CI 実装

## タスク

### 1. `.github/workflows/ci.yml` の作成

- `.github/workflows/` ディレクトリを作成する
- `ci.yml` を作成し、`requirements.md` に記載した要件をすべて満たすワークフローを記述する
  - トリガー: `pull_request`
  - ランナー: `ubuntu-24.04`
  - タイムアウト: 5分
  - Concurrency 設定
  - `permissions: contents: read`
  - Java 21 セットアップ（`actions/setup-java@v4`、`cache: 'maven'`）
  - `./mvnw clean verify` の実行

### 2. ワークフローが正しく動作することの確認

- `ci.yml` を含むプルリクエストを作成する
- GitHub の Actions タブでワークフローが起動・成功することを確認する
