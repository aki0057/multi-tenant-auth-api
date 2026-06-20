# タスクリスト：CLAUDE.md のドキュメント分離

## 完了条件

- `docs/` 配下に必要なファイルが揃っており、各ファイルに適切な内容が記述されている
- CLAUDE.md が「命令・制約・手順 + docs/ へのポインタ」のみの構成になっている
- CLAUDE.md から移動したコンテンツに重複・欠落がない

---

## タスク一覧

### 1. `docs/architecture.md` を新規作成
- [x] マルチテナント方式（シングルスキーマ、`tenant_id` FK、`tenantCode` で解決）を記述
- [x] ドメインモデルと JPA エンティティの分離方針を記述
- [x] MapStruct による変換の説明を記述

### 2. `docs/repository-structure.md` を新規作成
- [x] DDDレイヤーのディレクトリツリーを記述
- [x] 各ディレクトリの役割（Controller, UseCase, record, mapper 等）を記述

### 3. `docs/database-design.md` に追記
- [x] CLAUDE.md のテーブル構成テーブル（役割列）の情報を既存ファイルにマージ

### 4. `docs/development-guidelines.md` を新規作成
- [x] アノテーションプロセッサの順序（Lombok → MapStruct）を記述
- [x] ドキュメント分類ルール（永続的ドキュメント vs 作業単位ドキュメント）の説明を移動

### 5. `CLAUDE.md` を書き直し
- [x] `## アーキテクチャ` セクションを削除（docs/ に移動済みのため）
- [x] `## プロジェクト構造 > ドキュメントの分類` の説明文を削除（docs/ に移動済みのため）
- [x] `docs/` 配下ファイルへのポインタセクションを追加
- [x] セキュリティ制約（404/JWTスタブ）を残す
- [x] ステアリングプロセス（命名規則・作成手順）を残す
- [x] Commands を残す
