# multi-tenant-auth-api

マルチテナント対応権限管理API

## 概要

複数の企業・組織（テナント）が同一システムを共有しつつ、ユーザーデータを単一スキーマ内で分離して管理する認証認可API。  
JWT（アクセストークン＋リフレッシュトークン）によるステートフルな認証管理を実装する。

## 技術スタック

| カテゴリ      | 技術                          |
|-----------|-----------------------------|
| Language  | Java 21                     |
| Framework | Spring Boot 3.5.14          |
| Security  | Spring Security / JJWT      |
| Database  | PostgreSQL                  |
| ORM       | Spring Data JPA / MapStruct |
| API Docs  | Swagger UI                  |
| Build     | Maven                       |

## ドキュメント

- [データベース設計](docs/database-design.md) — ER図・テーブル定義・設計決定事項
