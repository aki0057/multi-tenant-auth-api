# アーキテクチャ設計

## 採用アーキテクチャ

DDD のレイヤードアーキテクチャを採用している。各レイヤーの詳細は `docs/repository-structure.md` を参照。

---

## マルチテナント方式

**シングルスキーマ方式**を採用。テナントごとにスキーマやDBを分けず、全テーブルに `tenant_id` FK を持たせる。

- クエリ時に `WHERE tenant_id = ?` を必ず付与してデータを分離する
- リクエストはテナント識別子として `tenantCode`（`tenants.code` カラムの値）を受け取る
- アプリケーション内で `tenantCode` → DB上の `tenant_id` に解決してから処理する

---

## ドメインモデル設計方針

ドメインモデルは JPA エンティティと**完全分離**した純粋な Java `record` として定義する。

- **ドメイン層**（`domain/model/`）: JPA アノテーションを持たない Java record
- **Infrastructure 層**（`infrastructure/persistence/`）: JPA エンティティ（`@Entity`）を定義
- **変換**: MapStruct を使って JPA エンティティ ↔ ドメインモデルを相互変換する（`infrastructure/persistence/mapper/`）

この分離により、ドメイン層が JPA に依存せず、ビジネスロジックのテストが容易になる。
