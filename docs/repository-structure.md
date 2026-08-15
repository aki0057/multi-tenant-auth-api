# プロジェクト構造

## ディレクトリ構成

```
src/main/java/.../
├── presentation/          ← Controller, Request/Response DTO
│   ├── advice/            ← 例外ハンドラー
│   └── filter/            ← JWT フィルター
├── application/           ← UseCase サービス, Command オブジェクト
├── domain/
│   ├── model/             ← Domain オブジェクト
│   └── repository/        ← リポジトリインターフェース
├── infrastructure/
│   └── persistence/
│       ├── entity/        ← JPA エンティティ
│       ├── repository/    ← リポジトリ実装
│       └── mapper/        ← MapStruct によるエンティティ ↔ Domain オブジェクト変換
└── config/                ← Spring 設定クラス
```

## ファイル配置ルール

| 種別                               | 配置先                                      |
|----------------------------------|------------------------------------------|
| Controller（`@RestController`）    | `presentation/`                          |
| Request / Response DTO           | `presentation/`                          |
| 例外ハンドラー（`@RestControllerAdvice`） | `presentation/advice/`                   |
| JWT フィルター                        | `presentation/filter/`                   |
| UseCase クラス（`@Service`）          | `application/`                           |
| Command オブジェクト                   | `application/`                           |
| Domain オブジェクト                    | `domain/model/`                          |
| リポジトリインターフェース                    | `domain/repository/`                     |
| JPA エンティティ（`@Entity`）            | `infrastructure/persistence/entity/`     |
| リポジトリ実装（`@Repository`）           | `infrastructure/persistence/repository/` |
| MapStruct マッパー                   | `infrastructure/persistence/mapper/`     |
| Spring 設定クラス（`@Configuration`）   | `config/`                                |
