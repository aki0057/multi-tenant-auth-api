package io.github.aki0057.multitenant.auth.domain.model;

import io.github.aki0057.multitenant.auth.domain.model.vo.Email;
import io.github.aki0057.multitenant.auth.domain.model.vo.PasswordHash;
import io.github.aki0057.multitenant.auth.domain.model.vo.Role;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantCode;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;

/**
 * User ドメインモデル。
 * JPAエンティティとは分離し、ビジネスロジックのみを持つ純粋なPOJO。
 */
public record User(
        UserId id,
        TenantCode tenantCode,
        Email email,
        PasswordHash passwordHash,
        Role role,
        boolean active
) {}

