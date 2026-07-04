package io.github.aki0057.multitenant.auth.domain.model;

import io.github.aki0057.multitenant.auth.domain.model.vo.RefreshTokenId;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantId;
import io.github.aki0057.multitenant.auth.domain.model.vo.TokenHash;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    /** 判定基準となる現在時刻（固定値）。 */
    private static final Instant NOW = Instant.parse("2026-07-03T00:00:00Z");

    /** SHA-256 の hex 文字列形式（64 桁の 16 進数）に準拠した有効なハッシュ値。 */
    private static final String VALID_HASH =
            "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3d740c96f6f7e1a2b3c4d5e6f";

    private RefreshToken newToken(Instant expiresAt, boolean revoked) {
        return new RefreshToken(
                new RefreshTokenId(1L),
                new TenantId(1L),
                new UserId(1L),
                new TokenHash(VALID_HASH),
                expiresAt,
                revoked
        );
    }

    // ---------------------------------------------------------------
    // 正常系
    // ---------------------------------------------------------------
    @Test
    @DisplayName("正常系: revoke() は revoked=true の新インスタンスを返し、他のフィールドは元の値を引き継ぎ、元のインスタンスは変更されない。")
    void revoke_returnsRevokedInstance() {
        RefreshToken original = newToken(NOW.plusSeconds(60), false);

        RefreshToken revoked = original.revoke();

        assertThat(revoked.revoked()).isTrue();
        assertThat(revoked.id()).isEqualTo(original.id());
        assertThat(revoked.tenantId()).isEqualTo(original.tenantId());
        assertThat(revoked.userId()).isEqualTo(original.userId());
        assertThat(revoked.tokenHash()).isEqualTo(original.tokenHash());
        assertThat(revoked.expiresAt()).isEqualTo(original.expiresAt());
        // 元のインスタンスは変更されない（immutable であること）
        assertThat(original.revoked()).isFalse();
    }

    @Test
    @DisplayName("正常系: 未失効（revoked=false）かつ未期限切れ（有効期限が現在時刻より未来）の場合、isValid は true を返す。")
    void isValid_notRevokedAndNotExpired() {
        RefreshToken token = newToken(NOW.plusSeconds(60), false);

        assertThat(token.isValid(NOW)).isTrue();
    }

    // ---------------------------------------------------------------
    // 異常系
    // ---------------------------------------------------------------
    @Test
    @DisplayName("異常系: 失効済み（revoked=true）の場合、期限切れでなくても isValid は false を返す。")
    void isValid_revoked() {
        RefreshToken token = newToken(NOW.plusSeconds(60), true);

        assertThat(token.isValid(NOW)).isFalse();
    }

    @Test
    @DisplayName("異常系: 期限切れ（有効期限が現在時刻より過去）の場合、未失効でも isValid は false を返す。")
    void isValid_expired() {
        RefreshToken token = newToken(NOW.minusSeconds(60), false);

        assertThat(token.isValid(NOW)).isFalse();
    }
}
