package io.github.aki0057.multitenant.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 現在時刻取得用の {@link Clock} を DI コンテナへ登録する設定クラス。
 * 時刻をモック可能にし、有効期限判定などのテスト容易性を確保する。
 */
@Configuration
public class ClockConfig {

    /**
     * UTC のシステムクロックを提供する。
     *
     * @return UTC のシステムクロック
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
