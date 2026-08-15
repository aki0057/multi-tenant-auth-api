package io.github.aki0057.multitenant.auth.infrastructure.persistence.mapper;

import io.github.aki0057.multitenant.auth.domain.model.User;
import io.github.aki0057.multitenant.auth.domain.model.vo.*;
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
     *
     * <p>ネストした参照および名前の異なるフィールドは明示的にマッピングする。</p>
     *
     * <p>それ以外のフィールドは下記の型変換メソッドを MapStruct が自動適用する。</p>
     *
     * @param entity 変換元の JPA エンティティ
     * @return 変換後の User ドメインモデル
     */
    @Mapping(source = "id", target = "userId")
    @Mapping(source = "tenant.id", target = "tenantId")
    @Mapping(source = "tenant.code", target = "tenantCode")
    @Mapping(source = "active", target = "userIdIsActive")
    @Mapping(source = "tenant.active", target = "tenantIdIsActive")
    User toDomain(UserJpaEntity entity);

    default TenantId     toTenantId(Long value)      { return new TenantId(value); }
    default UserId       toUserId(Long value)      { return new UserId(value); }
    default TenantCode   toTenantCode(String value) { return new TenantCode(value); }
    default Email        toEmail(String value)       { return new Email(value); }
    default PasswordHash toPasswordHash(String value){ return new PasswordHash(value); }
    default Role         toRole(String value)        { return new Role(value); }
}

