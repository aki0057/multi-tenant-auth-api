package io.github.aki0057.multitenant.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI（Swagger UI）のメタデータと認証スキームを定義する設定クラス。
 * {@code @Bean OpenAPI} 方式で OpenAPI ドキュメントを構築する。
 * <p>
 * Bearer(JWT) の SecurityScheme（{@code bearerAuth}）を 1 件宣言し、
 * Swagger UI に「Authorize」ボタンを表示させる。ただしグローバルな
 * security requirement（{@code addSecurityItem}）は設定しない。現状の
 * 実装済みエンドポイント（{@code POST /login}・{@code POST /refresh}）は
 * いずれも {@code permitAll} のため Bearer を要求しない。認証必須エンドポイントが
 * 将来追加された際に、各エンドポイントへ {@code @SecurityRequirement("bearerAuth")}
 * を付与して要求する前提で、ここではスキーム宣言のみを行う。
 */
@Configuration
public class OpenApiConfig {

    /** Swagger UI の「Authorize」ボタンに対応する SecurityScheme 名。 */
    private static final String BEARER_AUTH = "bearerAuth";

    /**
     * OpenAPI ドキュメントを構築する。
     * API メタデータ（title / version / description）と、Bearer(JWT) の
     * SecurityScheme（{@code bearerAuth}: type=HTTP, scheme=bearer, bearerFormat=JWT）を
     * 登録した {@link OpenAPI} を返す。グローバルな security requirement は付与しない。
     *
     * @return 構築した {@link OpenAPI} ドキュメント
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Multi-Tenant Auth API")
                        .version("v1")
                        .description("マルチテナント対応の認証認可 API。JWT アクセストークンと "
                                + "リフレッシュトークン（Cookie 方式）による認証を提供する。"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
