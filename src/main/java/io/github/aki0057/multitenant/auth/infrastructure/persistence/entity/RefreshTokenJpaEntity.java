package io.github.aki0057.multitenant.auth.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * refresh_tokens テーブルの JPA エンティティ。
 *
 * <p>日時カラム（{@code created_at} / {@code updated_at}）は
 * {@link AuditingEntityListener} と {@link CreatedDate} / {@link LastModifiedDate} で
 * 自動管理する。{@code created_by} / {@code updated_by} は Auditing 対象外であり、
 * RefreshTokenMapper が明示設定する。</p>
 *
 * <p>{@code created_at} / {@code created_by} は {@code updatable = false} とし、
 * 失効更新（UPDATE）の SET 句に含めないことで、DB の既存値の上書き・NOT NULL 違反を防ぐ。</p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "refresh_tokens")
public class RefreshTokenJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantJpaEntity tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "is_revoked", nullable = false)
    private boolean revoked;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;
}
