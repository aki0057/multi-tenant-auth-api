package io.github.aki0057.multitenant.auth.infrastructure.persistence.repository;

import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA リポジトリ。
 * トークンハッシュで refresh_tokens テーブルを検索する。
 */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {

    /**
     * token_hash でリフレッシュトークンを検索する。
     * Spring Data JPA がメソッド名からクエリを自動生成する。
     *
     * @param tokenHash 生トークンの SHA-256 ハッシュ値（hex 文字列）
     * @return 該当するリフレッシュトークンエンティティ（存在しない場合は空）
     */
    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);
}
