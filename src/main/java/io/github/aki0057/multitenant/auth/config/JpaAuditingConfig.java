package io.github.aki0057.multitenant.auth.config;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Spring Data JPA の Auditing 機能を有効化する設定クラス。
 *
 * <p>{@code @EntityListeners(AuditingEntityListener.class)} を付与した JPA エンティティの
 * 日時カラム（{@code @CreatedDate} / {@code @LastModifiedDate}）を永続化時に自動設定する。</p>
 *
 * <p>自動管理の対象は日時カラムのみとし、AuditorAware Bean は定義しない
 * （{@code created_by} / {@code updated_by} は Auditing 対象外として各マッパーが明示設定する）。</p>
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    /**
     * Auditing の日時カラムへ設定する現在日時を {@link OffsetDateTime} で供給する。
     *
     * @return 現在日時（{@link OffsetDateTime}）を返す {@link DateTimeProvider}
     */
    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}
