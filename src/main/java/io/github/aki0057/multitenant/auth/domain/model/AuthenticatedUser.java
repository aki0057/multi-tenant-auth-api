package io.github.aki0057.multitenant.auth.domain.model;

import io.github.aki0057.multitenant.auth.domain.model.vo.Role;
import io.github.aki0057.multitenant.auth.domain.model.vo.TenantId;
import io.github.aki0057.multitenant.auth.domain.model.vo.UserId;

/**
 * JWT アクセストークンのクレームから復元される認証情報キャリア。
 *
 * <p>{@link User} 集約のように DB から復元する集約ルートではなく、
 * トークンのクレームから構築できる最小情報のみを保持する軽量な Value Object である。
 * バリデーションを一切持たず、{@code AccessTokenVerifier#verify} の戻り値として、
 * フィルタが SecurityContext の principal にセットするために用いる。</p>
 *
 * @param userId   ユーザー ID（{@code sub} クレーム由来）
 * @param tenantId テナント ID（{@code tenantId} クレーム由来）
 * @param role     ロール（{@code role} クレーム由来）
 */
public record AuthenticatedUser(UserId userId, TenantId tenantId, Role role) {}
