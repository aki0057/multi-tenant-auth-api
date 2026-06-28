package io.github.aki0057.multitenant.auth.domain.model;

import io.github.aki0057.multitenant.auth.domain.model.vo.*;

/**
 * User ドメインモデル。
 * JPAエンティティとは分離し、ビジネスロジックのみを持つ純粋なPOJO。
 */
public record User(
        UserId userId,
        TenantId tenantId,
        TenantCode tenantCode,
        Email email,
        PasswordHash passwordHash,
        Role role,
        boolean userIdIsActive,
        boolean tenantIdIsActive
) {}

