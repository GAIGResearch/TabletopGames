package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.artifacts.dsl.CapabilityNotationParser;
import javax.inject.Inject;

/**
 * A catalog of dependencies accessible via the {@code libs} extension.
 */
@NonNullApi
public class LibrariesForLibs extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final ComLibraryAccessors laccForComLibraryAccessors = new ComLibraryAccessors(owner);
    private final CommonsLibraryAccessors laccForCommonsLibraryAccessors = new CommonsLibraryAccessors(owner);
    private final IoLibraryAccessors laccForIoLibraryAccessors = new IoLibraryAccessors(owner);
    private final JunitLibraryAccessors laccForJunitLibraryAccessors = new JunitLibraryAccessors(owner);
    private final OrgLibraryAccessors laccForOrgLibraryAccessors = new OrgLibraryAccessors(owner);
    private final TechLibraryAccessors laccForTechLibraryAccessors = new TechLibraryAccessors(owner);
    private final TwLibraryAccessors laccForTwLibraryAccessors = new TwLibraryAccessors(owner);
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibs(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) {
        super(config, providers, objects, attributesFactory, capabilityNotationParser);
    }

    /**
     * Group of libraries at <b>com</b>
     */
    public ComLibraryAccessors getCom() {
        return laccForComLibraryAccessors;
    }

    /**
     * Group of libraries at <b>commons</b>
     */
    public CommonsLibraryAccessors getCommons() {
        return laccForCommonsLibraryAccessors;
    }

    /**
     * Group of libraries at <b>io</b>
     */
    public IoLibraryAccessors getIo() {
        return laccForIoLibraryAccessors;
    }

    /**
     * Group of libraries at <b>junit</b>
     */
    public JunitLibraryAccessors getJunit() {
        return laccForJunitLibraryAccessors;
    }

    /**
     * Group of libraries at <b>org</b>
     */
    public OrgLibraryAccessors getOrg() {
        return laccForOrgLibraryAccessors;
    }

    /**
     * Group of libraries at <b>tech</b>
     */
    public TechLibraryAccessors getTech() {
        return laccForTechLibraryAccessors;
    }

    /**
     * Group of libraries at <b>tw</b>
     */
    public TwLibraryAccessors getTw() {
        return laccForTwLibraryAccessors;
    }

    /**
     * Group of versions at <b>versions</b>
     */
    public VersionAccessors getVersions() {
        return vaccForVersionAccessors;
    }

    /**
     * Group of bundles at <b>bundles</b>
     */
    public BundleAccessors getBundles() {
        return baccForBundleAccessors;
    }

    /**
     * Group of plugins at <b>plugins</b>
     */
    public PluginAccessors getPlugins() {
        return paccForPluginAccessors;
    }

    public static class ComLibraryAccessors extends SubDependencyFactory {
        private final ComGithubLibraryAccessors laccForComGithubLibraryAccessors = new ComGithubLibraryAccessors(owner);
        private final ComGlobalmentorLibraryAccessors laccForComGlobalmentorLibraryAccessors = new ComGlobalmentorLibraryAccessors(owner);
        private final ComGoogleLibraryAccessors laccForComGoogleLibraryAccessors = new ComGoogleLibraryAccessors(owner);
        private final ComGooglecodeLibraryAccessors laccForComGooglecodeLibraryAccessors = new ComGooglecodeLibraryAccessors(owner);

        public ComLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.github</b>
         */
        public ComGithubLibraryAccessors getGithub() {
            return laccForComGithubLibraryAccessors;
        }

        /**
         * Group of libraries at <b>com.globalmentor</b>
         */
        public ComGlobalmentorLibraryAccessors getGlobalmentor() {
            return laccForComGlobalmentorLibraryAccessors;
        }

        /**
         * Group of libraries at <b>com.google</b>
         */
        public ComGoogleLibraryAccessors getGoogle() {
            return laccForComGoogleLibraryAccessors;
        }

        /**
         * Group of libraries at <b>com.googlecode</b>
         */
        public ComGooglecodeLibraryAccessors getGooglecode() {
            return laccForComGooglecodeLibraryAccessors;
        }

    }

    public static class ComGithubLibraryAccessors extends SubDependencyFactory {
        private final ComGithubDavidmotenLibraryAccessors laccForComGithubDavidmotenLibraryAccessors = new ComGithubDavidmotenLibraryAccessors(owner);
        private final ComGithubHopshackleLibraryAccessors laccForComGithubHopshackleLibraryAccessors = new ComGithubHopshackleLibraryAccessors(owner);

        public ComGithubLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.github.davidmoten</b>
         */
        public ComGithubDavidmotenLibraryAccessors getDavidmoten() {
            return laccForComGithubDavidmotenLibraryAccessors;
        }

        /**
         * Group of libraries at <b>com.github.hopshackle</b>
         */
        public ComGithubHopshackleLibraryAccessors getHopshackle() {
            return laccForComGithubHopshackleLibraryAccessors;
        }

    }

    public static class ComGithubDavidmotenLibraryAccessors extends SubDependencyFactory {
        private final ComGithubDavidmotenWordLibraryAccessors laccForComGithubDavidmotenWordLibraryAccessors = new ComGithubDavidmotenWordLibraryAccessors(owner);

        public ComGithubDavidmotenLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.github.davidmoten.word</b>
         */
        public ComGithubDavidmotenWordLibraryAccessors getWord() {
            return laccForComGithubDavidmotenWordLibraryAccessors;
        }

    }

    public static class ComGithubDavidmotenWordLibraryAccessors extends SubDependencyFactory {

        public ComGithubDavidmotenWordLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>wrap</b> with <b>com.github.davidmoten:word-wrap</b> coordinates and
         * with version reference <b>com.github.davidmoten.word.wrap</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getWrap() {
            return create("com.github.davidmoten.word.wrap");
        }

    }

    public static class ComGithubHopshackleLibraryAccessors extends SubDependencyFactory {

        public ComGithubHopshackleLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>ntbea</b> with <b>com.github.hopshackle:NTBEA</b> coordinates and
         * with version reference <b>com.github.hopshackle.ntbea</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getNtbea() {
            return create("com.github.hopshackle.ntbea");
        }

    }

    public static class ComGlobalmentorLibraryAccessors extends SubDependencyFactory {
        private final ComGlobalmentorHadoopLibraryAccessors laccForComGlobalmentorHadoopLibraryAccessors = new ComGlobalmentorHadoopLibraryAccessors(owner);

        public ComGlobalmentorLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.globalmentor.hadoop</b>
         */
        public ComGlobalmentorHadoopLibraryAccessors getHadoop() {
            return laccForComGlobalmentorHadoopLibraryAccessors;
        }

    }

    public static class ComGlobalmentorHadoopLibraryAccessors extends SubDependencyFactory {
        private final ComGlobalmentorHadoopBareLibraryAccessors laccForComGlobalmentorHadoopBareLibraryAccessors = new ComGlobalmentorHadoopBareLibraryAccessors(owner);

        public ComGlobalmentorHadoopLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.globalmentor.hadoop.bare</b>
         */
        public ComGlobalmentorHadoopBareLibraryAccessors getBare() {
            return laccForComGlobalmentorHadoopBareLibraryAccessors;
        }

    }

    public static class ComGlobalmentorHadoopBareLibraryAccessors extends SubDependencyFactory {
        private final ComGlobalmentorHadoopBareNakedLibraryAccessors laccForComGlobalmentorHadoopBareNakedLibraryAccessors = new ComGlobalmentorHadoopBareNakedLibraryAccessors(owner);

        public ComGlobalmentorHadoopBareLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.globalmentor.hadoop.bare.naked</b>
         */
        public ComGlobalmentorHadoopBareNakedLibraryAccessors getNaked() {
            return laccForComGlobalmentorHadoopBareNakedLibraryAccessors;
        }

    }

    public static class ComGlobalmentorHadoopBareNakedLibraryAccessors extends SubDependencyFactory {
        private final ComGlobalmentorHadoopBareNakedLocalLibraryAccessors laccForComGlobalmentorHadoopBareNakedLocalLibraryAccessors = new ComGlobalmentorHadoopBareNakedLocalLibraryAccessors(owner);

        public ComGlobalmentorHadoopBareNakedLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.globalmentor.hadoop.bare.naked.local</b>
         */
        public ComGlobalmentorHadoopBareNakedLocalLibraryAccessors getLocal() {
            return laccForComGlobalmentorHadoopBareNakedLocalLibraryAccessors;
        }

    }

    public static class ComGlobalmentorHadoopBareNakedLocalLibraryAccessors extends SubDependencyFactory {

        public ComGlobalmentorHadoopBareNakedLocalLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>fs</b> with <b>com.globalmentor:hadoop-bare-naked-local-fs</b> coordinates and
         * with version reference <b>com.globalmentor.hadoop.bare.naked.local.fs</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getFs() {
            return create("com.globalmentor.hadoop.bare.naked.local.fs");
        }

    }

    public static class ComGoogleLibraryAccessors extends SubDependencyFactory {
        private final ComGoogleCodeLibraryAccessors$1 laccForComGoogleCodeLibraryAccessors$1 = new ComGoogleCodeLibraryAccessors$1(owner);

        public ComGoogleLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.google.code</b>
         */
        public ComGoogleCodeLibraryAccessors$1 getCode() {
            return laccForComGoogleCodeLibraryAccessors$1;
        }

    }

    public static class ComGoogleCodeLibraryAccessors$1 extends SubDependencyFactory {
        private final ComGoogleCodeGsonLibraryAccessors laccForComGoogleCodeGsonLibraryAccessors = new ComGoogleCodeGsonLibraryAccessors(owner);

        public ComGoogleCodeLibraryAccessors$1(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.google.code.gson</b>
         */
        public ComGoogleCodeGsonLibraryAccessors getGson() {
            return laccForComGoogleCodeGsonLibraryAccessors;
        }

    }

    public static class ComGoogleCodeGsonLibraryAccessors extends SubDependencyFactory {

        public ComGoogleCodeGsonLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>gson</b> with <b>com.google.code.gson:gson</b> coordinates and
         * with version reference <b>com.google.code.gson.gson</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getGson() {
            return create("com.google.code.gson.gson");
        }

    }

    public static class ComGooglecodeLibraryAccessors extends SubDependencyFactory {
        private final ComGooglecodeJsonLibraryAccessors laccForComGooglecodeJsonLibraryAccessors = new ComGooglecodeJsonLibraryAccessors(owner);

        public ComGooglecodeLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.googlecode.json</b>
         */
        public ComGooglecodeJsonLibraryAccessors getJson() {
            return laccForComGooglecodeJsonLibraryAccessors;
        }

    }

    public static class ComGooglecodeJsonLibraryAccessors extends SubDependencyFactory {
        private final ComGooglecodeJsonSimpleLibraryAccessors laccForComGooglecodeJsonSimpleLibraryAccessors = new ComGooglecodeJsonSimpleLibraryAccessors(owner);

        public ComGooglecodeJsonLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.googlecode.json.simple</b>
         */
        public ComGooglecodeJsonSimpleLibraryAccessors getSimple() {
            return laccForComGooglecodeJsonSimpleLibraryAccessors;
        }

    }

    public static class ComGooglecodeJsonSimpleLibraryAccessors extends SubDependencyFactory {
        private final ComGooglecodeJsonSimpleJsonLibraryAccessors laccForComGooglecodeJsonSimpleJsonLibraryAccessors = new ComGooglecodeJsonSimpleJsonLibraryAccessors(owner);

        public ComGooglecodeJsonSimpleLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.googlecode.json.simple.json</b>
         */
        public ComGooglecodeJsonSimpleJsonLibraryAccessors getJson() {
            return laccForComGooglecodeJsonSimpleJsonLibraryAccessors;
        }

    }

    public static class ComGooglecodeJsonSimpleJsonLibraryAccessors extends SubDependencyFactory {

        public ComGooglecodeJsonSimpleJsonLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>simple</b> with <b>com.googlecode.json-simple:json-simple</b> coordinates and
         * with version reference <b>com.googlecode.json.simple.json.simple</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSimple() {
            return create("com.googlecode.json.simple.json.simple");
        }

    }

    public static class CommonsLibraryAccessors extends SubDependencyFactory {
        private final CommonsIoLibraryAccessors laccForCommonsIoLibraryAccessors = new CommonsIoLibraryAccessors(owner);

        public CommonsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>commons.io</b>
         */
        public CommonsIoLibraryAccessors getIo() {
            return laccForCommonsIoLibraryAccessors;
        }

    }

    public static class CommonsIoLibraryAccessors extends SubDependencyFactory {
        private final CommonsIoCommonsLibraryAccessors laccForCommonsIoCommonsLibraryAccessors = new CommonsIoCommonsLibraryAccessors(owner);

        public CommonsIoLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>commons.io.commons</b>
         */
        public CommonsIoCommonsLibraryAccessors getCommons() {
            return laccForCommonsIoCommonsLibraryAccessors;
        }

    }

    public static class CommonsIoCommonsLibraryAccessors extends SubDependencyFactory {

        public CommonsIoCommonsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>io</b> with <b>commons-io:commons-io</b> coordinates and
         * with version reference <b>commons.io.commons.io</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getIo() {
            return create("commons.io.commons.io");
        }

    }

    public static class IoLibraryAccessors extends SubDependencyFactory {
        private final IoHumbleLibraryAccessors laccForIoHumbleLibraryAccessors = new IoHumbleLibraryAccessors(owner);

        public IoLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>io.humble</b>
         */
        public IoHumbleLibraryAccessors getHumble() {
            return laccForIoHumbleLibraryAccessors;
        }

    }

    public static class IoHumbleLibraryAccessors extends SubDependencyFactory {
        private final IoHumbleHumbleLibraryAccessors laccForIoHumbleHumbleLibraryAccessors = new IoHumbleHumbleLibraryAccessors(owner);

        public IoHumbleLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>io.humble.humble</b>
         */
        public IoHumbleHumbleLibraryAccessors getHumble() {
            return laccForIoHumbleHumbleLibraryAccessors;
        }

    }

    public static class IoHumbleHumbleLibraryAccessors extends SubDependencyFactory {
        private final IoHumbleHumbleVideoLibraryAccessors laccForIoHumbleHumbleVideoLibraryAccessors = new IoHumbleHumbleVideoLibraryAccessors(owner);

        public IoHumbleHumbleLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>io.humble.humble.video</b>
         */
        public IoHumbleHumbleVideoLibraryAccessors getVideo() {
            return laccForIoHumbleHumbleVideoLibraryAccessors;
        }

    }

    public static class IoHumbleHumbleVideoLibraryAccessors extends SubDependencyFactory {

        public IoHumbleHumbleVideoLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>all</b> with <b>io.humble:humble-video-all</b> coordinates and
         * with version reference <b>io.humble.humble.video.all</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getAll() {
            return create("io.humble.humble.video.all");
        }

    }

    public static class JunitLibraryAccessors extends SubDependencyFactory {

        public JunitLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>junit</b> with <b>junit:junit</b> coordinates and
         * with version reference <b>junit.junit</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJunit() {
            return create("junit.junit");
        }

    }

    public static class OrgLibraryAccessors extends SubDependencyFactory {
        private final OrgApacheLibraryAccessors laccForOrgApacheLibraryAccessors = new OrgApacheLibraryAccessors(owner);
        private final OrgKnowmLibraryAccessors laccForOrgKnowmLibraryAccessors = new OrgKnowmLibraryAccessors(owner);
        private final OrgTestngLibraryAccessors laccForOrgTestngLibraryAccessors = new OrgTestngLibraryAccessors(owner);

        public OrgLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.apache</b>
         */
        public OrgApacheLibraryAccessors getApache() {
            return laccForOrgApacheLibraryAccessors;
        }

        /**
         * Group of libraries at <b>org.knowm</b>
         */
        public OrgKnowmLibraryAccessors getKnowm() {
            return laccForOrgKnowmLibraryAccessors;
        }

        /**
         * Group of libraries at <b>org.testng</b>
         */
        public OrgTestngLibraryAccessors getTestng() {
            return laccForOrgTestngLibraryAccessors;
        }

    }

    public static class OrgApacheLibraryAccessors extends SubDependencyFactory {
        private final OrgApacheCommonsLibraryAccessors laccForOrgApacheCommonsLibraryAccessors = new OrgApacheCommonsLibraryAccessors(owner);
        private final OrgApacheSparkLibraryAccessors laccForOrgApacheSparkLibraryAccessors = new OrgApacheSparkLibraryAccessors(owner);

        public OrgApacheLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.apache.commons</b>
         */
        public OrgApacheCommonsLibraryAccessors getCommons() {
            return laccForOrgApacheCommonsLibraryAccessors;
        }

        /**
         * Group of libraries at <b>org.apache.spark</b>
         */
        public OrgApacheSparkLibraryAccessors getSpark() {
            return laccForOrgApacheSparkLibraryAccessors;
        }

    }

    public static class OrgApacheCommonsLibraryAccessors extends SubDependencyFactory {
        private final OrgApacheCommonsCommonsLibraryAccessors laccForOrgApacheCommonsCommonsLibraryAccessors = new OrgApacheCommonsCommonsLibraryAccessors(owner);

        public OrgApacheCommonsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.apache.commons.commons</b>
         */
        public OrgApacheCommonsCommonsLibraryAccessors getCommons() {
            return laccForOrgApacheCommonsCommonsLibraryAccessors;
        }

    }

    public static class OrgApacheCommonsCommonsLibraryAccessors extends SubDependencyFactory {

        public OrgApacheCommonsCommonsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>lang3</b> with <b>org.apache.commons:commons-lang3</b> coordinates and
         * with version reference <b>org.apache.commons.commons.lang3</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getLang3() {
            return create("org.apache.commons.commons.lang3");
        }

    }

    public static class OrgApacheSparkLibraryAccessors extends SubDependencyFactory {
        private final OrgApacheSparkSparkLibraryAccessors laccForOrgApacheSparkSparkLibraryAccessors = new OrgApacheSparkSparkLibraryAccessors(owner);

        public OrgApacheSparkLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.apache.spark.spark</b>
         */
        public OrgApacheSparkSparkLibraryAccessors getSpark() {
            return laccForOrgApacheSparkSparkLibraryAccessors;
        }

    }

    public static class OrgApacheSparkSparkLibraryAccessors extends SubDependencyFactory {
        private final OrgApacheSparkSparkCoreLibraryAccessors laccForOrgApacheSparkSparkCoreLibraryAccessors = new OrgApacheSparkSparkCoreLibraryAccessors(owner);
        private final OrgApacheSparkSparkMllibLibraryAccessors laccForOrgApacheSparkSparkMllibLibraryAccessors = new OrgApacheSparkSparkMllibLibraryAccessors(owner);
        private final OrgApacheSparkSparkSqlLibraryAccessors laccForOrgApacheSparkSparkSqlLibraryAccessors = new OrgApacheSparkSparkSqlLibraryAccessors(owner);

        public OrgApacheSparkSparkLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.apache.spark.spark.core</b>
         */
        public OrgApacheSparkSparkCoreLibraryAccessors getCore() {
            return laccForOrgApacheSparkSparkCoreLibraryAccessors;
        }

        /**
         * Group of libraries at <b>org.apache.spark.spark.mllib</b>
         */
        public OrgApacheSparkSparkMllibLibraryAccessors getMllib() {
            return laccForOrgApacheSparkSparkMllibLibraryAccessors;
        }

        /**
         * Group of libraries at <b>org.apache.spark.spark.sql</b>
         */
        public OrgApacheSparkSparkSqlLibraryAccessors getSql() {
            return laccForOrgApacheSparkSparkSqlLibraryAccessors;
        }

    }

    public static class OrgApacheSparkSparkCoreLibraryAccessors extends SubDependencyFactory {
        private final OrgApacheSparkSparkCoreV2LibraryAccessors laccForOrgApacheSparkSparkCoreV2LibraryAccessors = new OrgApacheSparkSparkCoreV2LibraryAccessors(owner);

        public OrgApacheSparkSparkCoreLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.apache.spark.spark.core.v2</b>
         */
        public OrgApacheSparkSparkCoreV2LibraryAccessors getV2() {
            return laccForOrgApacheSparkSparkCoreV2LibraryAccessors;
        }

    }

    public static class OrgApacheSparkSparkCoreV2LibraryAccessors extends SubDependencyFactory {

        public OrgApacheSparkSparkCoreV2LibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>v13</b> with <b>org.apache.spark:spark-core_2.13</b> coordinates and
         * with version reference <b>org.apache.spark.spark.core.v2.v13</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getV13() {
            return create("org.apache.spark.spark.core.v2.v13");
        }

    }

    public static class OrgApacheSparkSparkMllibLibraryAccessors extends SubDependencyFactory {
        private final OrgApacheSparkSparkMllibV2LibraryAccessors laccForOrgApacheSparkSparkMllibV2LibraryAccessors = new OrgApacheSparkSparkMllibV2LibraryAccessors(owner);

        public OrgApacheSparkSparkMllibLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.apache.spark.spark.mllib.v2</b>
         */
        public OrgApacheSparkSparkMllibV2LibraryAccessors getV2() {
            return laccForOrgApacheSparkSparkMllibV2LibraryAccessors;
        }

    }

    public static class OrgApacheSparkSparkMllibV2LibraryAccessors extends SubDependencyFactory {

        public OrgApacheSparkSparkMllibV2LibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>v13</b> with <b>org.apache.spark:spark-mllib_2.13</b> coordinates and
         * with version reference <b>org.apache.spark.spark.mllib.v2.v13</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getV13() {
            return create("org.apache.spark.spark.mllib.v2.v13");
        }

    }

    public static class OrgApacheSparkSparkSqlLibraryAccessors extends SubDependencyFactory {
        private final OrgApacheSparkSparkSqlV2LibraryAccessors laccForOrgApacheSparkSparkSqlV2LibraryAccessors = new OrgApacheSparkSparkSqlV2LibraryAccessors(owner);

        public OrgApacheSparkSparkSqlLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.apache.spark.spark.sql.v2</b>
         */
        public OrgApacheSparkSparkSqlV2LibraryAccessors getV2() {
            return laccForOrgApacheSparkSparkSqlV2LibraryAccessors;
        }

    }

    public static class OrgApacheSparkSparkSqlV2LibraryAccessors extends SubDependencyFactory {

        public OrgApacheSparkSparkSqlV2LibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>v13</b> with <b>org.apache.spark:spark-sql_2.13</b> coordinates and
         * with version reference <b>org.apache.spark.spark.sql.v2.v13</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getV13() {
            return create("org.apache.spark.spark.sql.v2.v13");
        }

    }

    public static class OrgKnowmLibraryAccessors extends SubDependencyFactory {
        private final OrgKnowmXchartLibraryAccessors laccForOrgKnowmXchartLibraryAccessors = new OrgKnowmXchartLibraryAccessors(owner);

        public OrgKnowmLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.knowm.xchart</b>
         */
        public OrgKnowmXchartLibraryAccessors getXchart() {
            return laccForOrgKnowmXchartLibraryAccessors;
        }

    }

    public static class OrgKnowmXchartLibraryAccessors extends SubDependencyFactory {

        public OrgKnowmXchartLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>xchart</b> with <b>org.knowm.xchart:xchart</b> coordinates and
         * with version reference <b>org.knowm.xchart.xchart</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getXchart() {
            return create("org.knowm.xchart.xchart");
        }

    }

    public static class OrgTestngLibraryAccessors extends SubDependencyFactory {

        public OrgTestngLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>testng</b> with <b>org.testng:testng</b> coordinates and
         * with version reference <b>org.testng.testng</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getTestng() {
            return create("org.testng.testng");
        }

    }

    public static class TechLibraryAccessors extends SubDependencyFactory {
        private final TechTablesawLibraryAccessors laccForTechTablesawLibraryAccessors = new TechTablesawLibraryAccessors(owner);

        public TechLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>tech.tablesaw</b>
         */
        public TechTablesawLibraryAccessors getTablesaw() {
            return laccForTechTablesawLibraryAccessors;
        }

    }

    public static class TechTablesawLibraryAccessors extends SubDependencyFactory {
        private final TechTablesawTablesawLibraryAccessors laccForTechTablesawTablesawLibraryAccessors = new TechTablesawTablesawLibraryAccessors(owner);

        public TechTablesawLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>tech.tablesaw.tablesaw</b>
         */
        public TechTablesawTablesawLibraryAccessors getTablesaw() {
            return laccForTechTablesawTablesawLibraryAccessors;
        }

    }

    public static class TechTablesawTablesawLibraryAccessors extends SubDependencyFactory {

        public TechTablesawTablesawLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>core</b> with <b>tech.tablesaw:tablesaw-core</b> coordinates and
         * with version reference <b>tech.tablesaw.tablesaw.core</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("tech.tablesaw.tablesaw.core");
        }

        /**
         * Dependency provider for <b>excel</b> with <b>tech.tablesaw:tablesaw-excel</b> coordinates and
         * with version reference <b>tech.tablesaw.tablesaw.excel</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getExcel() {
            return create("tech.tablesaw.tablesaw.excel");
        }

        /**
         * Dependency provider for <b>html</b> with <b>tech.tablesaw:tablesaw-html</b> coordinates and
         * with version reference <b>tech.tablesaw.tablesaw.html</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getHtml() {
            return create("tech.tablesaw.tablesaw.html");
        }

        /**
         * Dependency provider for <b>json</b> with <b>tech.tablesaw:tablesaw-json</b> coordinates and
         * with version reference <b>tech.tablesaw.tablesaw.json</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJson() {
            return create("tech.tablesaw.tablesaw.json");
        }

        /**
         * Dependency provider for <b>jsplot</b> with <b>tech.tablesaw:tablesaw-jsplot</b> coordinates and
         * with version reference <b>tech.tablesaw.tablesaw.jsplot</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJsplot() {
            return create("tech.tablesaw.tablesaw.jsplot");
        }

    }

    public static class TwLibraryAccessors extends SubDependencyFactory {
        private final TwEduLibraryAccessors laccForTwEduLibraryAccessors = new TwEduLibraryAccessors(owner);

        public TwLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>tw.edu</b>
         */
        public TwEduLibraryAccessors getEdu() {
            return laccForTwEduLibraryAccessors;
        }

    }

    public static class TwEduLibraryAccessors extends SubDependencyFactory {
        private final TwEduNtuLibraryAccessors laccForTwEduNtuLibraryAccessors = new TwEduNtuLibraryAccessors(owner);

        public TwEduLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>tw.edu.ntu</b>
         */
        public TwEduNtuLibraryAccessors getNtu() {
            return laccForTwEduNtuLibraryAccessors;
        }

    }

    public static class TwEduNtuLibraryAccessors extends SubDependencyFactory {
        private final TwEduNtuCsieLibraryAccessors laccForTwEduNtuCsieLibraryAccessors = new TwEduNtuCsieLibraryAccessors(owner);

        public TwEduNtuLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>tw.edu.ntu.csie</b>
         */
        public TwEduNtuCsieLibraryAccessors getCsie() {
            return laccForTwEduNtuCsieLibraryAccessors;
        }

    }

    public static class TwEduNtuCsieLibraryAccessors extends SubDependencyFactory {

        public TwEduNtuCsieLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>libsvm</b> with <b>tw.edu.ntu.csie:libsvm</b> coordinates and
         * with version reference <b>tw.edu.ntu.csie.libsvm</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getLibsvm() {
            return create("tw.edu.ntu.csie.libsvm");
        }

    }

    public static class VersionAccessors extends VersionFactory  {

        private final ComVersionAccessors vaccForComVersionAccessors = new ComVersionAccessors(providers, config);
        private final CommonsVersionAccessors vaccForCommonsVersionAccessors = new CommonsVersionAccessors(providers, config);
        private final IoVersionAccessors vaccForIoVersionAccessors = new IoVersionAccessors(providers, config);
        private final JunitVersionAccessors vaccForJunitVersionAccessors = new JunitVersionAccessors(providers, config);
        private final OrgVersionAccessors vaccForOrgVersionAccessors = new OrgVersionAccessors(providers, config);
        private final TechVersionAccessors vaccForTechVersionAccessors = new TechVersionAccessors(providers, config);
        private final TwVersionAccessors vaccForTwVersionAccessors = new TwVersionAccessors(providers, config);
        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com</b>
         */
        public ComVersionAccessors getCom() {
            return vaccForComVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.commons</b>
         */
        public CommonsVersionAccessors getCommons() {
            return vaccForCommonsVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.io</b>
         */
        public IoVersionAccessors getIo() {
            return vaccForIoVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.junit</b>
         */
        public JunitVersionAccessors getJunit() {
            return vaccForJunitVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.org</b>
         */
        public OrgVersionAccessors getOrg() {
            return vaccForOrgVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.tech</b>
         */
        public TechVersionAccessors getTech() {
            return vaccForTechVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.tw</b>
         */
        public TwVersionAccessors getTw() {
            return vaccForTwVersionAccessors;
        }

    }

    public static class ComVersionAccessors extends VersionFactory  {

        private final ComGithubVersionAccessors vaccForComGithubVersionAccessors = new ComGithubVersionAccessors(providers, config);
        private final ComGlobalmentorVersionAccessors vaccForComGlobalmentorVersionAccessors = new ComGlobalmentorVersionAccessors(providers, config);
        private final ComGoogleVersionAccessors vaccForComGoogleVersionAccessors = new ComGoogleVersionAccessors(providers, config);
        private final ComGooglecodeVersionAccessors vaccForComGooglecodeVersionAccessors = new ComGooglecodeVersionAccessors(providers, config);
        public ComVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.github</b>
         */
        public ComGithubVersionAccessors getGithub() {
            return vaccForComGithubVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.com.globalmentor</b>
         */
        public ComGlobalmentorVersionAccessors getGlobalmentor() {
            return vaccForComGlobalmentorVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.com.google</b>
         */
        public ComGoogleVersionAccessors getGoogle() {
            return vaccForComGoogleVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.com.googlecode</b>
         */
        public ComGooglecodeVersionAccessors getGooglecode() {
            return vaccForComGooglecodeVersionAccessors;
        }

    }

    public static class ComGithubVersionAccessors extends VersionFactory  {

        private final ComGithubDavidmotenVersionAccessors vaccForComGithubDavidmotenVersionAccessors = new ComGithubDavidmotenVersionAccessors(providers, config);
        private final ComGithubHopshackleVersionAccessors vaccForComGithubHopshackleVersionAccessors = new ComGithubHopshackleVersionAccessors(providers, config);
        public ComGithubVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.github.davidmoten</b>
         */
        public ComGithubDavidmotenVersionAccessors getDavidmoten() {
            return vaccForComGithubDavidmotenVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.com.github.hopshackle</b>
         */
        public ComGithubHopshackleVersionAccessors getHopshackle() {
            return vaccForComGithubHopshackleVersionAccessors;
        }

    }

    public static class ComGithubDavidmotenVersionAccessors extends VersionFactory  {

        private final ComGithubDavidmotenWordVersionAccessors vaccForComGithubDavidmotenWordVersionAccessors = new ComGithubDavidmotenWordVersionAccessors(providers, config);
        public ComGithubDavidmotenVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.github.davidmoten.word</b>
         */
        public ComGithubDavidmotenWordVersionAccessors getWord() {
            return vaccForComGithubDavidmotenWordVersionAccessors;
        }

    }

    public static class ComGithubDavidmotenWordVersionAccessors extends VersionFactory  {

        public ComGithubDavidmotenWordVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>com.github.davidmoten.word.wrap</b> with value <b>0.1.9</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getWrap() { return getVersion("com.github.davidmoten.word.wrap"); }

    }

    public static class ComGithubHopshackleVersionAccessors extends VersionFactory  {

        public ComGithubHopshackleVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>com.github.hopshackle.ntbea</b> with value <b>0.3.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getNtbea() { return getVersion("com.github.hopshackle.ntbea"); }

    }

    public static class ComGlobalmentorVersionAccessors extends VersionFactory  {

        private final ComGlobalmentorHadoopVersionAccessors vaccForComGlobalmentorHadoopVersionAccessors = new ComGlobalmentorHadoopVersionAccessors(providers, config);
        public ComGlobalmentorVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.globalmentor.hadoop</b>
         */
        public ComGlobalmentorHadoopVersionAccessors getHadoop() {
            return vaccForComGlobalmentorHadoopVersionAccessors;
        }

    }

    public static class ComGlobalmentorHadoopVersionAccessors extends VersionFactory  {

        private final ComGlobalmentorHadoopBareVersionAccessors vaccForComGlobalmentorHadoopBareVersionAccessors = new ComGlobalmentorHadoopBareVersionAccessors(providers, config);
        public ComGlobalmentorHadoopVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.globalmentor.hadoop.bare</b>
         */
        public ComGlobalmentorHadoopBareVersionAccessors getBare() {
            return vaccForComGlobalmentorHadoopBareVersionAccessors;
        }

    }

    public static class ComGlobalmentorHadoopBareVersionAccessors extends VersionFactory  {

        private final ComGlobalmentorHadoopBareNakedVersionAccessors vaccForComGlobalmentorHadoopBareNakedVersionAccessors = new ComGlobalmentorHadoopBareNakedVersionAccessors(providers, config);
        public ComGlobalmentorHadoopBareVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.globalmentor.hadoop.bare.naked</b>
         */
        public ComGlobalmentorHadoopBareNakedVersionAccessors getNaked() {
            return vaccForComGlobalmentorHadoopBareNakedVersionAccessors;
        }

    }

    public static class ComGlobalmentorHadoopBareNakedVersionAccessors extends VersionFactory  {

        private final ComGlobalmentorHadoopBareNakedLocalVersionAccessors vaccForComGlobalmentorHadoopBareNakedLocalVersionAccessors = new ComGlobalmentorHadoopBareNakedLocalVersionAccessors(providers, config);
        public ComGlobalmentorHadoopBareNakedVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.globalmentor.hadoop.bare.naked.local</b>
         */
        public ComGlobalmentorHadoopBareNakedLocalVersionAccessors getLocal() {
            return vaccForComGlobalmentorHadoopBareNakedLocalVersionAccessors;
        }

    }

    public static class ComGlobalmentorHadoopBareNakedLocalVersionAccessors extends VersionFactory  {

        public ComGlobalmentorHadoopBareNakedLocalVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>com.globalmentor.hadoop.bare.naked.local.fs</b> with value <b>0.1.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getFs() { return getVersion("com.globalmentor.hadoop.bare.naked.local.fs"); }

    }

    public static class ComGoogleVersionAccessors extends VersionFactory  {

        private final ComGoogleCodeVersionAccessors$1 vaccForComGoogleCodeVersionAccessors$1 = new ComGoogleCodeVersionAccessors$1(providers, config);
        public ComGoogleVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.google.code</b>
         */
        public ComGoogleCodeVersionAccessors$1 getCode() {
            return vaccForComGoogleCodeVersionAccessors$1;
        }

    }

    public static class ComGoogleCodeVersionAccessors$1 extends VersionFactory  {

        private final ComGoogleCodeGsonVersionAccessors vaccForComGoogleCodeGsonVersionAccessors = new ComGoogleCodeGsonVersionAccessors(providers, config);
        public ComGoogleCodeVersionAccessors$1(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.google.code.gson</b>
         */
        public ComGoogleCodeGsonVersionAccessors getGson() {
            return vaccForComGoogleCodeGsonVersionAccessors;
        }

    }

    public static class ComGoogleCodeGsonVersionAccessors extends VersionFactory  {

        public ComGoogleCodeGsonVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>com.google.code.gson.gson</b> with value <b>2.9.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getGson() { return getVersion("com.google.code.gson.gson"); }

    }

    public static class ComGooglecodeVersionAccessors extends VersionFactory  {

        private final ComGooglecodeJsonVersionAccessors vaccForComGooglecodeJsonVersionAccessors = new ComGooglecodeJsonVersionAccessors(providers, config);
        public ComGooglecodeVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.googlecode.json</b>
         */
        public ComGooglecodeJsonVersionAccessors getJson() {
            return vaccForComGooglecodeJsonVersionAccessors;
        }

    }

    public static class ComGooglecodeJsonVersionAccessors extends VersionFactory  {

        private final ComGooglecodeJsonSimpleVersionAccessors vaccForComGooglecodeJsonSimpleVersionAccessors = new ComGooglecodeJsonSimpleVersionAccessors(providers, config);
        public ComGooglecodeJsonVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.googlecode.json.simple</b>
         */
        public ComGooglecodeJsonSimpleVersionAccessors getSimple() {
            return vaccForComGooglecodeJsonSimpleVersionAccessors;
        }

    }

    public static class ComGooglecodeJsonSimpleVersionAccessors extends VersionFactory  {

        private final ComGooglecodeJsonSimpleJsonVersionAccessors vaccForComGooglecodeJsonSimpleJsonVersionAccessors = new ComGooglecodeJsonSimpleJsonVersionAccessors(providers, config);
        public ComGooglecodeJsonSimpleVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.googlecode.json.simple.json</b>
         */
        public ComGooglecodeJsonSimpleJsonVersionAccessors getJson() {
            return vaccForComGooglecodeJsonSimpleJsonVersionAccessors;
        }

    }

    public static class ComGooglecodeJsonSimpleJsonVersionAccessors extends VersionFactory  {

        public ComGooglecodeJsonSimpleJsonVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>com.googlecode.json.simple.json.simple</b> with value <b>1.1.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getSimple() { return getVersion("com.googlecode.json.simple.json.simple"); }

    }

    public static class CommonsVersionAccessors extends VersionFactory  {

        private final CommonsIoVersionAccessors vaccForCommonsIoVersionAccessors = new CommonsIoVersionAccessors(providers, config);
        public CommonsVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.commons.io</b>
         */
        public CommonsIoVersionAccessors getIo() {
            return vaccForCommonsIoVersionAccessors;
        }

    }

    public static class CommonsIoVersionAccessors extends VersionFactory  {

        private final CommonsIoCommonsVersionAccessors vaccForCommonsIoCommonsVersionAccessors = new CommonsIoCommonsVersionAccessors(providers, config);
        public CommonsIoVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.commons.io.commons</b>
         */
        public CommonsIoCommonsVersionAccessors getCommons() {
            return vaccForCommonsIoCommonsVersionAccessors;
        }

    }

    public static class CommonsIoCommonsVersionAccessors extends VersionFactory  {

        public CommonsIoCommonsVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>commons.io.commons.io</b> with value <b>2.7</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getIo() { return getVersion("commons.io.commons.io"); }

    }

    public static class IoVersionAccessors extends VersionFactory  {

        private final IoHumbleVersionAccessors vaccForIoHumbleVersionAccessors = new IoHumbleVersionAccessors(providers, config);
        public IoVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.io.humble</b>
         */
        public IoHumbleVersionAccessors getHumble() {
            return vaccForIoHumbleVersionAccessors;
        }

    }

    public static class IoHumbleVersionAccessors extends VersionFactory  {

        private final IoHumbleHumbleVersionAccessors vaccForIoHumbleHumbleVersionAccessors = new IoHumbleHumbleVersionAccessors(providers, config);
        public IoHumbleVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.io.humble.humble</b>
         */
        public IoHumbleHumbleVersionAccessors getHumble() {
            return vaccForIoHumbleHumbleVersionAccessors;
        }

    }

    public static class IoHumbleHumbleVersionAccessors extends VersionFactory  {

        private final IoHumbleHumbleVideoVersionAccessors vaccForIoHumbleHumbleVideoVersionAccessors = new IoHumbleHumbleVideoVersionAccessors(providers, config);
        public IoHumbleHumbleVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.io.humble.humble.video</b>
         */
        public IoHumbleHumbleVideoVersionAccessors getVideo() {
            return vaccForIoHumbleHumbleVideoVersionAccessors;
        }

    }

    public static class IoHumbleHumbleVideoVersionAccessors extends VersionFactory  {

        public IoHumbleHumbleVideoVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>io.humble.humble.video.all</b> with value <b>0.3.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getAll() { return getVersion("io.humble.humble.video.all"); }

    }

    public static class JunitVersionAccessors extends VersionFactory  {

        public JunitVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>junit.junit</b> with value <b>4.13.2</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getJunit() { return getVersion("junit.junit"); }

    }

    public static class OrgVersionAccessors extends VersionFactory  {

        private final OrgApacheVersionAccessors vaccForOrgApacheVersionAccessors = new OrgApacheVersionAccessors(providers, config);
        private final OrgKnowmVersionAccessors vaccForOrgKnowmVersionAccessors = new OrgKnowmVersionAccessors(providers, config);
        private final OrgTestngVersionAccessors vaccForOrgTestngVersionAccessors = new OrgTestngVersionAccessors(providers, config);
        public OrgVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.org.apache</b>
         */
        public OrgApacheVersionAccessors getApache() {
            return vaccForOrgApacheVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.org.knowm</b>
         */
        public OrgKnowmVersionAccessors getKnowm() {
            return vaccForOrgKnowmVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.org.testng</b>
         */
        public OrgTestngVersionAccessors getTestng() {
            return vaccForOrgTestngVersionAccessors;
        }

    }

    public static class OrgApacheVersionAccessors extends VersionFactory  {

        private final OrgApacheCommonsVersionAccessors vaccForOrgApacheCommonsVersionAccessors = new OrgApacheCommonsVersionAccessors(providers, config);
        private final OrgApacheSparkVersionAccessors vaccForOrgApacheSparkVersionAccessors = new OrgApacheSparkVersionAccessors(providers, config);
        public OrgApacheVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.org.apache.commons</b>
         */
        public OrgApacheCommonsVersionAccessors getCommons() {
            return vaccForOrgApacheCommonsVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.org.apache.spark</b>
         */
        public OrgApacheSparkVersionAccessors getSpark() {
            return vaccForOrgApacheSparkVersionAccessors;
        }

    }

    public static class OrgApacheCommonsVersionAccessors extends VersionFactory  {

        private final OrgApacheCommonsCommonsVersionAccessors vaccForOrgApacheCommonsCommonsVersionAccessors = new OrgApacheCommonsCommonsVersionAccessors(providers, config);
        public OrgApacheCommonsVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.org.apache.commons.commons</b>
         */
        public OrgApacheCommonsCommonsVersionAccessors getCommons() {
            return vaccForOrgApacheCommonsCommonsVersionAccessors;
        }

    }

    public static class OrgApacheCommonsCommonsVersionAccessors extends VersionFactory  {

        public OrgApacheCommonsCommonsVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>org.apache.commons.commons.lang3</b> with value <b>3.12.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getLang3() { return getVersion("org.apache.commons.commons.lang3"); }

    }

    public static class OrgApacheSparkVersionAccessors extends VersionFactory  {

        private final OrgApacheSparkSparkVersionAccessors vaccForOrgApacheSparkSparkVersionAccessors = new OrgApacheSparkSparkVersionAccessors(providers, config);
        public OrgApacheSparkVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.org.apache.spark.spark</b>
         */
        public OrgApacheSparkSparkVersionAccessors getSpark() {
            return vaccForOrgApacheSparkSparkVersionAccessors;
        }

    }

    public static class OrgApacheSparkSparkVersionAccessors extends VersionFactory  {

        private final OrgApacheSparkSparkCoreVersionAccessors vaccForOrgApacheSparkSparkCoreVersionAccessors = new OrgApacheSparkSparkCoreVersionAccessors(providers, config);
        private final OrgApacheSparkSparkMllibVersionAccessors vaccForOrgApacheSparkSparkMllibVersionAccessors = new OrgApacheSparkSparkMllibVersionAccessors(providers, config);
        private final OrgApacheSparkSparkSqlVersionAccessors vaccForOrgApacheSparkSparkSqlVersionAccessors = new OrgApacheSparkSparkSqlVersionAccessors(providers, config);
        public OrgApacheSparkSparkVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.org.apache.spark.spark.core</b>
         */
        public OrgApacheSparkSparkCoreVersionAccessors getCore() {
            return vaccForOrgApacheSparkSparkCoreVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.org.apache.spark.spark.mllib</b>
         */
        public OrgApacheSparkSparkMllibVersionAccessors getMllib() {
            return vaccForOrgApacheSparkSparkMllibVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.org.apache.spark.spark.sql</b>
         */
        public OrgApacheSparkSparkSqlVersionAccessors getSql() {
            return vaccForOrgApacheSparkSparkSqlVersionAccessors;
        }

    }

    public static class OrgApacheSparkSparkCoreVersionAccessors extends VersionFactory  {

        private final OrgApacheSparkSparkCoreV2VersionAccessors vaccForOrgApacheSparkSparkCoreV2VersionAccessors = new OrgApacheSparkSparkCoreV2VersionAccessors(providers, config);
        public OrgApacheSparkSparkCoreVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.org.apache.spark.spark.core.v2</b>
         */
        public OrgApacheSparkSparkCoreV2VersionAccessors getV2() {
            return vaccForOrgApacheSparkSparkCoreV2VersionAccessors;
        }

    }

    public static class OrgApacheSparkSparkCoreV2VersionAccessors extends VersionFactory  {

        public OrgApacheSparkSparkCoreV2VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>org.apache.spark.spark.core.v2.v13</b> with value <b>3.3.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getV13() { return getVersion("org.apache.spark.spark.core.v2.v13"); }

    }

    public static class OrgApacheSparkSparkMllibVersionAccessors extends VersionFactory  {

        private final OrgApacheSparkSparkMllibV2VersionAccessors vaccForOrgApacheSparkSparkMllibV2VersionAccessors = new OrgApacheSparkSparkMllibV2VersionAccessors(providers, config);
        public OrgApacheSparkSparkMllibVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.org.apache.spark.spark.mllib.v2</b>
         */
        public OrgApacheSparkSparkMllibV2VersionAccessors getV2() {
            return vaccForOrgApacheSparkSparkMllibV2VersionAccessors;
        }

    }

    public static class OrgApacheSparkSparkMllibV2VersionAccessors extends VersionFactory  {

        public OrgApacheSparkSparkMllibV2VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>org.apache.spark.spark.mllib.v2.v13</b> with value <b>3.3.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getV13() { return getVersion("org.apache.spark.spark.mllib.v2.v13"); }

    }

    public static class OrgApacheSparkSparkSqlVersionAccessors extends VersionFactory  {

        private final OrgApacheSparkSparkSqlV2VersionAccessors vaccForOrgApacheSparkSparkSqlV2VersionAccessors = new OrgApacheSparkSparkSqlV2VersionAccessors(providers, config);
        public OrgApacheSparkSparkSqlVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.org.apache.spark.spark.sql.v2</b>
         */
        public OrgApacheSparkSparkSqlV2VersionAccessors getV2() {
            return vaccForOrgApacheSparkSparkSqlV2VersionAccessors;
        }

    }

    public static class OrgApacheSparkSparkSqlV2VersionAccessors extends VersionFactory  {

        public OrgApacheSparkSparkSqlV2VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>org.apache.spark.spark.sql.v2.v13</b> with value <b>3.3.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getV13() { return getVersion("org.apache.spark.spark.sql.v2.v13"); }

    }

    public static class OrgKnowmVersionAccessors extends VersionFactory  {

        private final OrgKnowmXchartVersionAccessors vaccForOrgKnowmXchartVersionAccessors = new OrgKnowmXchartVersionAccessors(providers, config);
        public OrgKnowmVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.org.knowm.xchart</b>
         */
        public OrgKnowmXchartVersionAccessors getXchart() {
            return vaccForOrgKnowmXchartVersionAccessors;
        }

    }

    public static class OrgKnowmXchartVersionAccessors extends VersionFactory  {

        public OrgKnowmXchartVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>org.knowm.xchart.xchart</b> with value <b>3.8.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getXchart() { return getVersion("org.knowm.xchart.xchart"); }

    }

    public static class OrgTestngVersionAccessors extends VersionFactory  {

        public OrgTestngVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>org.testng.testng</b> with value <b>RELEASE</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getTestng() { return getVersion("org.testng.testng"); }

    }

    public static class TechVersionAccessors extends VersionFactory  {

        private final TechTablesawVersionAccessors vaccForTechTablesawVersionAccessors = new TechTablesawVersionAccessors(providers, config);
        public TechVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.tech.tablesaw</b>
         */
        public TechTablesawVersionAccessors getTablesaw() {
            return vaccForTechTablesawVersionAccessors;
        }

    }

    public static class TechTablesawVersionAccessors extends VersionFactory  {

        private final TechTablesawTablesawVersionAccessors vaccForTechTablesawTablesawVersionAccessors = new TechTablesawTablesawVersionAccessors(providers, config);
        public TechTablesawVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.tech.tablesaw.tablesaw</b>
         */
        public TechTablesawTablesawVersionAccessors getTablesaw() {
            return vaccForTechTablesawTablesawVersionAccessors;
        }

    }

    public static class TechTablesawTablesawVersionAccessors extends VersionFactory  {

        public TechTablesawTablesawVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>tech.tablesaw.tablesaw.core</b> with value <b>0.43.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getCore() { return getVersion("tech.tablesaw.tablesaw.core"); }

        /**
         * Version alias <b>tech.tablesaw.tablesaw.excel</b> with value <b>0.43.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getExcel() { return getVersion("tech.tablesaw.tablesaw.excel"); }

        /**
         * Version alias <b>tech.tablesaw.tablesaw.html</b> with value <b>0.43.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getHtml() { return getVersion("tech.tablesaw.tablesaw.html"); }

        /**
         * Version alias <b>tech.tablesaw.tablesaw.json</b> with value <b>0.43.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getJson() { return getVersion("tech.tablesaw.tablesaw.json"); }

        /**
         * Version alias <b>tech.tablesaw.tablesaw.jsplot</b> with value <b>0.43.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getJsplot() { return getVersion("tech.tablesaw.tablesaw.jsplot"); }

    }

    public static class TwVersionAccessors extends VersionFactory  {

        private final TwEduVersionAccessors vaccForTwEduVersionAccessors = new TwEduVersionAccessors(providers, config);
        public TwVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.tw.edu</b>
         */
        public TwEduVersionAccessors getEdu() {
            return vaccForTwEduVersionAccessors;
        }

    }

    public static class TwEduVersionAccessors extends VersionFactory  {

        private final TwEduNtuVersionAccessors vaccForTwEduNtuVersionAccessors = new TwEduNtuVersionAccessors(providers, config);
        public TwEduVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.tw.edu.ntu</b>
         */
        public TwEduNtuVersionAccessors getNtu() {
            return vaccForTwEduNtuVersionAccessors;
        }

    }

    public static class TwEduNtuVersionAccessors extends VersionFactory  {

        private final TwEduNtuCsieVersionAccessors vaccForTwEduNtuCsieVersionAccessors = new TwEduNtuCsieVersionAccessors(providers, config);
        public TwEduNtuVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.tw.edu.ntu.csie</b>
         */
        public TwEduNtuCsieVersionAccessors getCsie() {
            return vaccForTwEduNtuCsieVersionAccessors;
        }

    }

    public static class TwEduNtuCsieVersionAccessors extends VersionFactory  {

        public TwEduNtuCsieVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>tw.edu.ntu.csie.libsvm</b> with value <b>3.25</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getLibsvm() { return getVersion("tw.edu.ntu.csie.libsvm"); }

    }

    public static class BundleAccessors extends BundleFactory {

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

    }

    public static class PluginAccessors extends PluginFactory {

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

    }

}
