package io.github.aki0057.multitenant.auth;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * DDD レイヤードアーキテクチャの依存方向を静的に検査するアーキテクチャテスト。
 *
 * <p>ArchUnit を用いて、ソースコードのコンパイル時依存が定められたレイヤー順序を守っているかを
 * 検査する。各ルールは「禁止された依存を持つクラスが存在しないこと（{@code noClasses()...should()
 * .dependOnClassesThat()}）」を宣言し、違反があればテストが失敗する。宣言的ルールそのものが
 * 違反検出（異常系）を担い、現状の依存構造ではすべて成功（正常系）する。</p>
 *
 * <p>検査対象は main のクラスのみとし（{@link ImportOption.DoNotIncludeTests}）、横断的関心事である
 * {@code ..config..} パッケージとルート直下の {@code MultiTenantAuthApiApplication} は
 * ルールの検査対象（起点）から除外する（{@link ExcludeConfigAndApplication}）。config は複数レイヤーへ
 * 依存してよく、Application はブートストラップ用途のためである。</p>
 */
@AnalyzeClasses(
        packages = "io.github.aki0057.multitenant.auth",
        importOptions = {ImportOption.DoNotIncludeTests.class, ArchitectureTest.ExcludeConfigAndApplication.class})
class ArchitectureTest {

    /**
     * ルール1: domain は他レイヤー（presentation / application / infrastructure / config）に依存しない。
     *
     * <p>domain は最も内側のレイヤーであり、外側のいかなるレイヤーにも依存してはならない。</p>
     */
    @ArchTest
    static final ArchRule domainShouldNotDependOnOtherLayers =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..presentation..",
                            "..application..",
                            "..infrastructure..",
                            "..config..")
                    .as("domain は presentation / application / infrastructure / config に依存してはならない");

    /**
     * ルール2: domain はフレームワーク（Spring / Jakarta）に依存しない。
     *
     * <p>domain は純粋な業務ドメインを表現し、特定のフレームワークから独立していなければならない。</p>
     */
    @ArchTest
    static final ArchRule domainShouldNotDependOnFrameworks =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta..")
                    .as("domain は Spring / Jakarta などのフレームワークに依存してはならない");

    /**
     * ルール3: application は presentation / infrastructure / config に依存しない。
     *
     * <p>application が依存してよいのは内側の domain のみである。</p>
     */
    @ArchTest
    static final ArchRule applicationShouldOnlyDependOnDomain =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..presentation..",
                            "..infrastructure..",
                            "..config..")
                    .as("application は presentation / infrastructure / config に依存してはならない（domain のみ許可）");

    /**
     * ルール4: presentation は infrastructure / config に依存しない。
     *
     * <p>presentation が依存してよいのは内側の application / domain のみである。</p>
     */
    @ArchTest
    static final ArchRule presentationShouldNotDependOnInfrastructureOrConfig =
            noClasses()
                    .that().resideInAPackage("..presentation..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..infrastructure..",
                            "..config..")
                    .as("presentation は infrastructure / config に依存してはならない（application / domain のみ許可）");

    /**
     * ルール5: infrastructure は presentation / application / config に依存しない。
     *
     * <p>infrastructure が依存してよいのは domain のみである。</p>
     */
    @ArchTest
    static final ArchRule infrastructureShouldOnlyDependOnDomain =
            noClasses()
                    .that().resideInAPackage("..infrastructure..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..presentation..",
                            "..application..",
                            "..config..")
                    .as("infrastructure は presentation / application / config に依存してはならない");

    /**
     * ルール検査の起点から {@code ..config..} パッケージとルート直下の
     * {@code MultiTenantAuthApiApplication} を除外する {@link ImportOption}。
     *
     * <p>config は横断的関心事であり複数レイヤーへ依存してよく、Application はブートストラップ用途の
     * ため、いずれもレイヤー依存ルールの検査対象（起点）としない。なお除外はインポート対象からの
     * 除外であり、他レイヤーがこれらへ依存していないかの検査（依存先としての判定）には影響しない。</p>
     */
    static final class ExcludeConfigAndApplication implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return !location.contains("/config/")
                    && !location.contains("MultiTenantAuthApiApplication");
        }
    }
}
