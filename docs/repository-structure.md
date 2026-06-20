# プロジェクト構造

## ディレクトリ構成

```
src/main/java/.../
├── presentation/          ← Controller, Request/Response DTO, GlobalExceptionHandler
├── application/           ← UseCase サービス, Command オブジェクト
├── domain/
│   ├── model/             ← ドメインモデル（record）, Value Object（vo/）
│   └── repository/        ← リポジトリインターフェース（ドメイン層に属する）
├── infrastructure/
│   └── persistence/
│       ├── entity/        ← JPA エンティティ（ドメインモデルとは別クラス）
│       ├── repository/    ← リポジトリ実装（例: UserRepositoryImpl）
│       └── mapper/        ← MapStruct によるエンティティ ↔ ドメインモデル変換
├── filter/                ← JwtAuthenticationFilter
└── config/                ← SecurityConfig, PasswordEncoderConfig
```

## ファイル配置ルール

| 種別                               | 配置先                                      |
|----------------------------------|------------------------------------------|
| Controller                       | `presentation/`                          |
| Request / Response DTO           | `presentation/`                          |
| 例外ハンドラー（`@RestControllerAdvice`） | `presentation/`                          |
| UseCase クラス（`@Service`）          | `application/`                           |
| Command オブジェクト                   | `application/`                           |
| ドメインモデル（`record`）                | `domain/model/`                          |
| Value Object                     | `domain/model/vo/`                       |
| リポジトリインターフェース                    | `domain/repository/`                     |
| JPA エンティティ（`@Entity`）            | `infrastructure/persistence/entity/`     |
| リポジトリ実装（`@Repository`）           | `infrastructure/persistence/repository/` |
| MapStruct マッパー                   | `infrastructure/persistence/mapper/`     |
| JWT フィルター                        | `filter/`                                |
| Spring 設定クラス（`@Configuration`）   | `config/`                                |
