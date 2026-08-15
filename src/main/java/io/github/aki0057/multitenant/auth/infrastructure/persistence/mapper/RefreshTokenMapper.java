package io.github.aki0057.multitenant.auth.infrastructure.persistence.mapper;

import io.github.aki0057.multitenant.auth.domain.model.RefreshToken;
import io.github.aki0057.multitenant.auth.domain.model.vo.RefreshTokenId;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantId;
import io.github.aki0057.multitenant.auth.domain.model.vo.TokenHash;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.TenantJpaEntity;
import io.github.aki0057.multitenant.auth.infrastructure.persistence.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * RefreshTokenJpaEntity ↔ RefreshToken ドメインモデルの相互変換を担う MapStruct マッパー。
 * componentModel = "spring" により Spring Bean として自動登録される。
 *
 * <p>{@code @ManyToOne} 参照（tenant / user）の解決は行わない。ドメイン→エンティティ変換では
 * 呼び出し側（RefreshTokenRepositoryImpl）が解決済みの参照を引数として渡す。</p>
 */
@Mapper(componentModel = "spring")
public interface RefreshTokenMapper {

    /**
     * JPA エンティティをドメインモデルへ変換する。
     *
     * <p>ネストした参照は明示的にマッピングし、それ以外のフィールドは
     * 下記の型変換メソッドを MapStruct が自動適用する。
     * 日時カラム（createdAt / updatedAt）と createdBy / updatedBy は
     * ドメインモデルが保持しないため変換対象外。</p>
     *
     * @param entity 変換元の JPA エンティティ
     * @return 変換後の RefreshToken ドメインモデル
     */
    @Mapping(source = "tenant.id", target = "tenantId")
    @Mapping(source = "user.id", target = "userId")
    RefreshToken toDomain(RefreshTokenJpaEntity entity);

    /**
     * ドメインモデルを JPA エンティティへ変換する。
     *
     * <p>{@code @ManyToOne} の tenant / user は呼び出し側が解決済みの参照を渡す。
     * createdBy / updatedBy は Auditing 対象外のため、ドメインの userId を文字列化して
     * 明示設定する。日時カラム（createdAt / updatedAt）は JPA Auditing が
     * 自動管理するためマッピングしない。</p>
     *
     * @param domain 変換元の RefreshToken ドメインモデル（未永続化の場合 id は {@code null}）
     * @param tenant 呼び出し側で解決済みのテナント参照
     * @param user   呼び出し側で解決済みのユーザー参照
     * @return 変換後の JPA エンティティ
     */
    @Mapping(source = "domain.id", target = "id")
    @Mapping(source = "tenant", target = "tenant")
    @Mapping(source = "user", target = "user")
    @Mapping(source = "domain.tokenHash", target = "tokenHash")
    @Mapping(source = "domain.expiresAt", target = "expiresAt")
    @Mapping(source = "domain.revoked", target = "revoked")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", expression = "java(String.valueOf(domain.userId().value()))")
    @Mapping(target = "updatedBy", expression = "java(String.valueOf(domain.userId().value()))")
    RefreshTokenJpaEntity toEntity(RefreshToken domain, TenantJpaEntity tenant, UserJpaEntity user);

    default RefreshTokenId toRefreshTokenId(Long value)   { return new RefreshTokenId(value); }
    default TenantId       toTenantId(Long value)          { return new TenantId(value); }
    default UserId         toUserId(Long value)            { return new UserId(value); }
    default TokenHash      toTokenHash(String value)       { return new TokenHash(value); }
    default Instant        toInstant(OffsetDateTime value) { return value.toInstant(); }

    /**
     * 主キー VO を Long へ変換する。
     * 未永続化のドメインモデル（INSERT 経路）では id が {@code null} のため null セーフとする。
     *
     * @param id リフレッシュトークンの主キー VO（{@code null} 許容）
     * @return 主キー値（{@code id} が {@code null} の場合は {@code null}）
     */
    default Long toId(RefreshTokenId id) {
        return id == null ? null : id.value();
    }

    default String toTokenHashValue(TokenHash tokenHash) {
        return tokenHash.value();
    }

    default OffsetDateTime toOffsetDateTime(Instant value) {
        return value.atOffset(ZoneOffset.UTC);
    }
}
