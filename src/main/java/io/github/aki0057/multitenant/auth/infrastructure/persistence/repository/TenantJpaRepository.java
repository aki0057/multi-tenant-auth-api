package io.github.aki0057.multitenant.auth.infrastructure.persistence.repository;

import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.TenantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA リポジトリ。
 *
 * <p>{@code RefreshTokenRepositoryImpl#save} が {@code @ManyToOne} の tenant 参照を
 * {@link JpaRepository#getReferenceById(Object)}（追加の SELECT を発行しないプロキシ取得）で
 * 解決するためだけに存在する。カスタムクエリメソッドは追加しない。</p>
 */
public interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, Long> {
}
