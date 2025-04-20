package net.minecraftforge.gradleutils;

import groovy.lang.Closure;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

import java.io.File;
import java.util.function.Consumer;

/**
 * Contains various utilities for working with Gradle scripts.
 * <p>Projects that apply GradleUtils are given {@link GradleUtilsExtension.ForProject}</p>
 */
@SuppressWarnings("rawtypes") // public-facing Closures
public sealed interface GradleUtilsExtension permits GradleUtilsExtensionImpl, GradleUtilsExtension.ForProject {
    /** The name for this extension. */
    String NAME = "gradleutils";

    /**
     * A closure for the Forge maven to be passed into
     * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)}.
     * <pre><code>
     * repositories {
     *     maven fg.forgeMaven
     * }
     * </code></pre>
     *
     * @see #forgeMaven
     */
    Closure forgeMaven = closure((MavenArtifactRepository repo) -> {
        repo.setName("MinecraftForge");
        repo.setUrl("https://maven.minecraftforge.net/");
    });

    /**
     * A closure for the Forge releases maven to be passed into
     * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)}.
     * <pre><code>
     * repositories {
     *     maven fg.forgeReleaseMaven
     * }
     * </code></pre>
     *
     * @see #forgeMaven
     */
    Closure forgeReleaseMaven = closure((MavenArtifactRepository repo) -> {
        repo.setName("'MinecraftForge releases'");
        repo.setUrl("https://maven.minecraftforge.net/releases");
    });

    /**
     * A closure for the Forge snapshot maven to be passed into
     * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)}.
     * <pre><code>
     * repositories {
     *     maven fg.forgeSnapshotMaven
     * }
     * </code></pre>
     *
     * @see #forgeMaven
     */
    Closure getForgeSnapshotMaven = closure((MavenArtifactRepository repo) -> {
        repo.setName("MinecraftForge snapshots");
        repo.setUrl("https://maven.minecraftforge.net/snapshots");
    });

    /**
     * A closure for the Minecraft libraries maven to be passed into
     * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)}.
     * <pre><code>
     * repositories {
     *     maven fg.minecraftLibsMaven
     * }
     * </code></pre>
     */
    Closure getMinecraftLibsMaven = closure((MavenArtifactRepository repo) -> {
        repo.setName("Minecraft libraries");
        repo.setUrl("https://libraries.minecraft.net/");
    });

    /**
     * The GradleUtils extension for {@linkplain org.gradle.api.Project projects}, which include additional utilities
     * that are only available for them.
     * <p>When applied, GradleUtils will</p>
     * <ul>
     *     <li>Create a referenceable {@link PomUtils} instance in {@link #getPom()}.</li>
     *     <li>Register the {@code generateActionsWorkflow} task to the project for generating a default template GitHub
     *     Actions workflow.</li>
     *     <li>Register the {@code configureTeamCity} task to the project for working with TeamCity CI pipelines.</li>
     * </ul>
     *
     * @see GradleUtilsExtension
     */
    sealed interface ForProject extends GradleUtilsExtension permits GradleUtilsExtensionImpl.ForProject {
        /**
         * Utilities for working with a {@link org.gradle.api.publish.maven.MavenPom} for publishing artifacts.
         *
         * @return The POM utilities
         * @see PomUtils
         */
        PomUtils getPom();

        /**
         * Get a configuring closure to be passed into
         * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)} in a publishing block.
         * <p>This closure respects the current project's version in regard to publishing to a release or snapshot
         * repository.</p>
         * <p><strong>Important:</strong> The following environment variables must be set for this to work:</p>
         * <ul>
         *     <li>{@code MAVEN_USER}: Containing the username to use for authentication</li>
         *     <li>{@code MAVEN_PASSWORD}: Containing the password to use for authentication</li>
         * </ul>
         * <p>The following environment variables are optional:</p>
         * <ul>
         *     <li>{@code MAVEN_URL_RELEASE}: Containing the URL to use for the release repository</li>
         *     <li>{@code MAVEN_URL_SNAPSHOT}: Containing the URL to use for the snapshot repository</li>
         * </ul>
         * <p>If the required environment variables are not present, the output Maven will be a local folder named
         * {@code repo} on the root of the
         * {@linkplain org.gradle.api.file.ProjectLayout#getProjectDirectory() project directory}.</p>
         * <p>If the {@code MAVEN_URL_RELEASE} variable is not set, the Forge Maven will be used
         * ({@code https://maven.minecraftforge.net/}).</p>
         *
         * @return The closure
         */
        default Closure getPublishingForgeMaven() {
            return getPublishingForgeMaven("https://maven.minecraftforge.net/");
        }

        /**
         * Get a configuring closure to be passed into
         * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)} in a publishing block.
         * <p>This closure respects the current project's version in regard to publishing to a release or snapshot
         * repository.</p>
         * <p><strong>Important:</strong> The following environment variables must be set for this to work:</p>
         * <ul>
         *     <li>{@code MAVEN_USER}: Containing the username to use for authentication</li>
         *     <li>{@code MAVEN_PASSWORD}: Containing the password to use for authentication</li>
         * </ul>
         * <p>The following environment variables are optional:</p>
         * <ul>
         *     <li>{@code MAVEN_URL_RELEASE}: Containing the URL to use for the release repository</li>
         *     <li>{@code MAVEN_URL_SNAPSHOT}: Containing the URL to use for the snapshot repository</li>
         * </ul>
         * <p>If the required environment variables are not present, the output Maven will be a local folder named
         * {@code repo} on the root of the
         * {@linkplain org.gradle.api.file.ProjectLayout#getProjectDirectory() project directory}.</p>
         * <p>If the {@code MAVEN_URL_RELEASE} variable is not set, the passed in fallback URL will be used for the
         * release repository.</p>
         *
         * @param fallbackPublishingEndpoint The fallback URL for the release repository
         * @return The closure
         */
        Closure getPublishingForgeMaven(String fallbackPublishingEndpoint);

        /**
         * Get a configuring closure to be passed into
         * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)} in a publishing block.
         * <p>This closure respects the current project's version in regard to publishing to a release or snapshot
         * repository.</p>
         * <p><strong>Important:</strong> The following environment variables must be set for this to work:</p>
         * <ul>
         *     <li>{@code MAVEN_USER}: Containing the username to use for authentication</li>
         *     <li>{@code MAVEN_PASSWORD}: Containing the password to use for authentication</li>
         * </ul>
         * <p>The following environment variables are optional:</p>
         * <ul>
         *     <li>{@code MAVEN_URL_RELEASE}: Containing the URL to use for the release repository</li>
         *     <li>{@code MAVEN_URL_SNAPSHOT}: Containing the URL to use for the snapshot repository</li>
         * </ul>
         * <p>If the required environment variables are not present, the output Maven will be set to the given default
         * folder.</p>
         * <p>If the {@code MAVEN_URL_RELEASE} variable is not set, the passed in fallback URL will be used for the
         * release repository.</p>
         *
         * @param fallbackPublishingEndpoint The fallback URL for the release repository
         * @param defaultFolder              The default folder if the required maven information is not set
         * @return The closure
         */
        default Closure getPublishingForgeMaven(String fallbackPublishingEndpoint, File defaultFolder) {
            return getPublishingForgeMaven(fallbackPublishingEndpoint, defaultFolder, new File(defaultFolder.getAbsoluteFile().getParentFile(), "snapshots"));
        }

        /**
         * Get a configuring closure to be passed into
         * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Closure)} in a publishing block.
         * <p>This closure respects the current project's version in regard to publishing to a release or snapshot
         * repository.</p>
         * <p><strong>Important:</strong> The following environment variables must be set for this to work:</p>
         * <ul>
         *     <li>{@code MAVEN_USER}: Containing the username to use for authentication</li>
         *     <li>{@code MAVEN_PASSWORD}: Containing the password to use for authentication</li>
         * </ul>
         * <p>The following environment variables are optional:</p>
         * <ul>
         *     <li>{@code MAVEN_URL_RELEASE}: Containing the URL to use for the release repository</li>
         *     <li>{@code MAVEN_URL_SNAPSHOT}: Containing the URL to use for the snapshot repository</li>
         * </ul>
         * <p>If the required environment variables are not present, the output Maven will be set to the given default
         * folder.</p>
         * <p>If the {@code MAVEN_URL_RELEASE} variable is not set, the passed in fallback URL will be used for the
         * release repository.</p>
         *
         * @param fallbackPublishingEndpoint The fallback URL for the release repository
         * @param defaultFolder              The default folder if the required maven information is not set
         * @param defaultSnapshotFolder      The default folder for the snapshot repository if the required maven
         *                                   information is not set
         * @return The closure
         */
        Closure getPublishingForgeMaven(String fallbackPublishingEndpoint, File defaultFolder, File defaultSnapshotFolder);
    }

    private static <T> Closure closure(Consumer<T> consumer) {
        return Util.closure(GradleUtilsExtension.class, consumer);
    }
}
