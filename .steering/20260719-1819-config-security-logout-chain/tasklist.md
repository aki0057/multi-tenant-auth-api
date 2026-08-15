# tasklist

- [x] 作業対象を実装する（`SecurityConfig#refreshSecurityFilterChain` の `securityMatcher` に `/auth/logout` を追加し、`/auth/refresh` と `/auth/logout` の両方をマッチさせる。既存 `/auth/refresh` の挙動は変更しない）
- [x] Javadoc を更新する（対象チェーンが `/auth/refresh` と `/auth/logout` を保護する旨に記載を更新する）
