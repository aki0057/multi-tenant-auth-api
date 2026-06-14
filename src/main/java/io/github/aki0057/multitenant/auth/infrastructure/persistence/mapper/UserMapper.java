package io.github.aki0057.multitenant.auth.infrastructure.persistence.mapper;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.Email;
import io.github.aki0057.multitenant.auth.domain.model.vo.PasswordHash;
import io.github.aki0057.multitenant.auth.domain.model.vo.Role;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantCode;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * UserJpaEntity → User ドメインモデルの変換を担う MapStruct マッパー。
 * componentModel = "spring" により Spring Bean として自動登録される。
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * JPA エンティティをドメインモデルへ変換する。
     * tenant.code（ネストした参照）を tenantCode フィールドへマッピングする。
     * それ以外のフィールドは下記の型変換メソッドを MapStruct が自動適用する。
     */
    @Mapping(source = "tenant.code", target = "tenantCode")
    User toDomain(UserJpaEntity entity);

    default UserId       toUserId(Long value)      { return new UserId(value); }
    default TenantCode   toTenantCode(String value) { return new TenantCode(value); }
    default Email        toEmail(String value)       { return new Email(value); }
    default PasswordHash toPasswordHash(String value){ return new PasswordHash(value); }
    default Role         toRole(String value)        { return new Role(value); }
}

