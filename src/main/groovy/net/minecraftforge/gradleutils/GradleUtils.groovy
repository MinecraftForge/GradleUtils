/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import net.minecraftforge.gitver.api.GitVersion
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.storage.file.FileBasedConfig
import org.eclipse.jgit.transport.RemoteConfig
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.util.SystemReader
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.authentication.http.BasicAuthentication
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nullable

/**
 * Utility methods, usually for GradleUtils itself and is often delegated to from the extension.
 *
 * @see GradleUtilsExtension
 */
@CompileStatic
@Deprecated(forRemoval = true, since = '2.5')
@ApiStatus.ScheduledForRemoval(inVersion = '3.0')
class GradleUtils {
    static void ensureAfterEvaluate(Project project, Action<? super Project> action) {
        if (project.state.executed)
            action.execute project
        else
            project.afterEvaluate action
    }

    //@formatter:off
    @CompileDynamic
    private static void initDynamic() { String.metaClass.rsplit = GradleUtils.&rsplit }
    static { initDynamic() }
    //@formatter:on

    private static boolean rsplitDeprecationLogged
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    static @Nullable List<String> rsplit(@Nullable String input, String del, int limit = -1) {
        if (!rsplitDeprecationLogged) {
            println 'WARNING: Usage of GradleUtils/String.rsplit is DEPRECATED and will be removed in GradleUtils 3.0!'
            rsplitDeprecationLogged = true
        }

        rsplitInternal input, del, limit
    }

    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    private static @Nullable List<String> rsplitInternal(@Nullable String input, String del, int limit = -1) {
        if (input === null) return null
        List<String> lst = []
        int x = 0
        int idx
        String tmp = input
        while ((idx = tmp.lastIndexOf(del)) !== -1 && (limit === -1 || x++ < limit)) {
            lst.add(0, tmp.substring(idx + del.length(), tmp.length()))
            tmp = tmp.substring(0, idx)
        }
        lst.add(0, tmp)
        return lst
    }

    /** @deprecated Use {@link GitVersion#disableSystemConfig() */
    @Deprecated(forRemoval = true, since = '2.4')
    @CompileStatic
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    static class DisableSystemConfig extends SystemReader.Delegate {
        final SystemReader parent

        DisableSystemConfig(SystemReader parent) {
            super(parent)
            this.parent = parent

            println 'WARNING: Usage of GradleUtils.DisableSystemConfig is DEPRECATED and will be removed in GradleUtils 3.0! Consider using GitVersion.disableSystemConfig() instead.'
        }

        @Override
        FileBasedConfig openSystemConfig(Config parent, FS fs) {
            new FileBasedConfig(parent, null, fs) {
                @Override void load() {}

                @Override boolean isOutdated() { false }
            }
        }
    }

    private static boolean gitInfoDeprecationLogged
    /** @deprecated Use {@link GitVersion#getInfo()} via {@link net.minecraftforge.gradleutils.gitversion.GitVersionExtension#getVersion() GitVersionExtension.getVersion()} */
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    static Map<String, String> gitInfo(File dir, String... globFilters) {
        if (!gitInfoDeprecationLogged) {
            println 'WARNING: Usage of GradleUtils.gitInfo(File, String...) is DEPRECATED and will be removed in GradleUtils 3.0! Consider using GitVersion.disableSystemConfig() instead.'
            gitInfoDeprecationLogged = true
        }

        try (var version = GitVersion.builder().project(dir).strict(false).build()) {
            [
                dir          : version.gitDir.absolutePath,
                tag          : version.info.tag,
                offset       : version.info.offset,
                hash         : version.info.hash,
                branch       : version.info.branch,
                commit       : version.info.commit,
                abbreviatedId: version.info.abbreviatedId,
                url          : version.url
            ].tap { it.removeAll { it.value == null } }
        }
    }

    /**
     * Get a configuring action to be passed into
     * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Action) RepositoryHandler.maven(Action)} in a
     * publishing block.
     *
     * <strong>Important:</strong> The following environment variables must be set for this to work:
     * <ul>
     *     <li>{@code MAVEN_USER}: Containing the username to use for authentication</li>
     *     <li>{@code MAVEN_PASSWORD}: Containing the password to use for authentication</li>
     *     <li>{@code MAVEN_URL_RELEASE}: Containing the URL to use for the release repository</li>
     *     <li>{@code MAVEN_URL_SNAPSHOT}: Containing the URL to use for the snapshot repository</li>
     * </ul>
     *
     * @param project The project to setup publishing for
     * @param defaultFolder The default folder if the required maven information is not set
     * @return The action
     */
    static Action<? super MavenArtifactRepository> getPublishingForgeMaven(Project project, File defaultFolder = project.rootProject.file('repo')) {
        setupSnapshotCompatiblePublishing(project, 'https://maven.minecraftforge.net/', defaultFolder)
    }

    /**
     * Get a configuring action to be passed into
     * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Action) RepositoryHandler.maven(Action)} in a
     * publishing block. This action respects the current project's version, with regards to publishing to a release or
     * snapshot repository.
     * <p>
     * <strong>Important:</strong> The following environment variables must be set for this to work:
     * <ul>
     *     <li>{@code MAVEN_USER}: Containing the username to use for authentication</li>
     *     <li>{@code MAVEN_PASSWORD}: Containing the password to use for authentication</li>
     * </ul>
     * <p>
     * The following environment variables are optional:
     * <ul>
     *     <li>{@code MAVEN_URL_RELEASE}: Containing the URL to use for the release repository</li>
     *     <li>{@code MAVEN_URL_SNAPSHOT}: Containing the URL to use for the snapshot repository</li>
     * </ul>
     * <p>
     * If the {@code MAVEN_URL_RELEASE} variable is not set, the passed in fallback URL will be used for the release
     * repository (by default, this is {@code https://maven.minecraftforge.net/}). This is done to preserve backwards
     * compatibility with the old {@link #getPublishingForgeMaven(Project, File)} method.
     *
     * @param project The project to setup publishing for
     * @param defaultFolder The default folder if the required maven information is not set
     * @return The action
     */
    static Action<? super MavenArtifactRepository> setupSnapshotCompatiblePublishing(Project project, String fallbackPublishingEndpoint = 'https://maven.minecraftforge.net/', File defaultFolder = project.rootProject.file('repo'), File defaultSnapshotFolder = project.rootProject.file('snapshots')) {
        // make properties of what we use so gradle's cache is aware
        final snapshot = project.objects.property(Boolean).value project.providers.provider {
            project.version?.toString()?.endsWith('-SNAPSHOT')
        }

        // collecting all of our environment variables here so gradle's cache is aware
        final mavenUser = project.providers.environmentVariable 'MAVEN_USER'
        final mavenPassword = project.providers.environmentVariable 'MAVEN_PASSWORD'
        final mavenUrlRelease = project.providers.environmentVariable 'MAVEN_URL_RELEASE'
        final mavenUrlSnapshots = project.providers.environmentVariable 'MAVEN_URL_SNAPSHOTS'

        return { MavenArtifactRepository repo ->
            repo.name = 'forge'

            if (mavenUser.present && mavenPassword.present) {
                var publishingEndpoint = mavenUrlRelease.present ? mavenUrlRelease.get() : fallbackPublishingEndpoint

                repo.url = snapshot.getOrElse(false) && mavenUrlSnapshots.present
                    ? mavenUrlSnapshots.get()
                    : publishingEndpoint

                repo.authentication { authentication ->
                    authentication.create('basic', BasicAuthentication)
                }

                repo.credentials { credentials ->
                    credentials.username = mavenUser.get()
                    credentials.password = mavenPassword.get()
                }
            } else {
                repo.url = snapshot.getOrElse(false)
                    ? defaultSnapshotFolder.absoluteFile.toURI()
                    : defaultFolder.absoluteFile.toURI()
            }
        } as Action<? super MavenArtifactRepository>
    }

    /**
     * Get a configuring action for the Forge maven to be passed into
     * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Action) RepositoryHandler.maven(Action)} in a
     * repositories block.
     *
     * @return The action
     */
    static Closure getForgeMaven() {
        { MavenArtifactRepository repo ->
            repo.name = 'MinecraftForge'
            repo.url = 'https://maven.minecraftforge.net/'
        }
    }

    /**
     * Get a configuring action for the Forge releases maven to be passed into
     * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Action) RepositoryHandler.maven(Action)} in a
     * repositories block.
     *
     * @return The action
     */
    static Closure getForgeReleaseMaven() {
        { MavenArtifactRepository repo ->
            repo.name = 'MinecraftForge releases'
            repo.url = 'https://maven.minecraftforge.net/releases'
        }
    }

    /**
     * Get a configuring action for the Forge snapshots maven to be passed into
     * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Action) RepositoryHandler.maven(Action)} in a
     * repositories block.
     *
     * @return The action
     */
    static Closure getForgeSnapshotMaven() {
        { MavenArtifactRepository repo ->
            repo.name = 'MinecraftForge snapshots'
            repo.url = 'https://maven.minecraftforge.net/snapshots'
        }
    }

    /**
     * Get a configuring action for the Minecraft libraries maven to be passed into
     * {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(Action) RepositoryHandler.maven(Action)} in a
     * repositories block.
     *
     * @return The action
     */
    static Closure getMinecraftLibsMaven() {
        { MavenArtifactRepository repo ->
            repo.name = 'Minecraft libraries'
            repo.url = 'https://libraries.minecraft.net/'
        }
    }

    /**
     * Returns a version in the form {@code $tag.$offset}, e.g. 1.0.5
     *
     * @param info A git info object generated from {@code #gitInfo}
     * @return a version in the form {@code $tag.$offset}, e.g. 1.0.5
     * @deprecated Use {@link GitVersion#getTagOffset()} instead
     */
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    static String getTagOffsetVersion(Map<String, String> info) {
        "${info.tag}.${info.offset}"
    }

    /** @deprecated Filters can no longer be defined at configuration. Use the Git Version config. */
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    static String getFilteredTagOffsetVersion(Map<String, String> info, boolean prefix = false, String filter) {
        getTagOffsetVersion info
    }

    /**
     * Returns a version in the form {@code $tag.$offset}, optionally with the branch appended if it is not in the
     * defined list of allowed branches
     *
     * @param info A git info object generated from {@link #gitInfo(File, String ...)}
     * @param allowedBranches A list of allowed branches; the current branch is appended if not in this list
     * @return a version in the form {@code $tag.$offset} or {@code $tag.$offset-$branch}
     * @deprecated Use {@link GitVersion#getTagOffsetBranch(String ...)} via {@link net.minecraftforge.gradleutils.gitversion.GitVersionExtension#getVersion() GitVersionExtension.getVersion()}
     */
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    static String getTagOffsetBranchVersion(Map<String, String> info, String... allowedBranches) {
        if (!allowedBranches || allowedBranches.length === 0)
            allowedBranches = [null, 'master', 'main', 'HEAD']
        final version = getTagOffsetVersion(info)
        String branch = info.branch
        if (branch?.startsWith('pulls/'))
            branch = 'pr' + rsplitInternal(branch, '/', 1)[1]
        branch = branch?.replaceAll(/[\\\/]/, '-')
        return branch in allowedBranches ? version : "$version-${branch}"
    }

    /** @deprecated Filters can no longer be defined at configuration. Use the Git Version config. */
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    static String getFilteredTagOffsetBranchVersion(Map<String, String> info, boolean prefix = false, String filter, String... allowedBranches) {
        getTagOffsetBranchVersion info, allowedBranches
    }

    /**
     * Returns a version in the form {@code $mcVersion-$tag.$offset}, optionally with
     * the branch appended if it is not in the defined list of allowed branches
     *
     * @param info A git info object generated from {@code #gitInfo}
     * @param mcVersion The current minecraft version
     * @param allowedBranches A list of allowed branches; the current branch is appended if not in this list
     * @return a version in the form {@code $mcVersion-$tag.$offset} or {@code $mcVersion-$tag.$offset-$branch}
     * @deprecated Use {@link GitVersion#getMCTagOffsetBranch(String, String ...)} instead
     */
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    static String getMCTagOffsetBranchVersion(Map<String, String> info, String mcVersion, String... allowedBranches) {
        if (!allowedBranches || allowedBranches.length === 0)
            allowedBranches = [null, 'master', 'main', 'HEAD', mcVersion, mcVersion + '.0', mcVersion + '.x', rsplitInternal(mcVersion, '.', 1)[0] + '.x']

        "$mcVersion-${getTagOffsetBranchVersion(info, allowedBranches)}"
    }

    /** @deprecated Filters for GitVersion should be set early, using one of the methods in {@link GradleUtilsExtension} */
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    static String getFilteredMCTagOffsetBranchVersion(Map<String, String> info, boolean prefix = false, String filter, String mcVersion, String... allowedBranches) {
        getMCTagOffsetBranchVersion info, mcVersion, allowedBranches
    }

    /** @see net.minecraftforge.gitver.internal.GitUtils#buildProjectUrl(String) GitUtils.buildProjectUrl(String) */
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    static String buildProjectUrl(String project) {
        buildProjectUrl "MinecraftForge", project
    }

    /** @see net.minecraftforge.gitver.internal.GitUtils#buildProjectUrl(String, String) GitUtils.buildProjectUrl(String, String) */
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    static String buildProjectUrl(String organization, String project) {
        buildProjectUrlLogDeprecation()
        "https://github.com/${organization}/${project}"
    }

    /**
     * Identical to
     * {@link net.minecraftforge.gitver.internal.GitUtils#buildProjectUrl(Git) GitUtils.buildProjectUrl(Git)}. The only
     * difference is that this does not return {@code null} to preserve GradleUtils 2.x behavior.
     *
     * @deprecated Replaced by GitVersion, use {@link GitVersion#getUrl()} via {@link net.minecraftforge.gradleutils.gitversion.GitVersionExtension#getVersion() GitVersionExtension.getVersion()}
     */
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    static String buildProjectUrl(Git git) {
        buildProjectUrlLogDeprecation()

        List<RemoteConfig> remotes
        try {
            remotes = git.remoteList().call()
            if (remotes.isEmpty()) return ''
        } catch (GitAPIException ignored) {
            return ''
        }

        //Get the origin remote.
        var originRemote =
            remotes.find { // First try finding the remote that has MinecraftForge
                remote -> remote.URIs.find { it.toString().contains('MinecraftForge/') }
            } ?: remotes.find { // Ok, just get the origin then
                remote -> remote.name == 'origin'
            } ?: remotes.first() // No origin? Get whatever we can get our hands on

        var originUrls = originRemote.getURIs()
        if (originUrls.empty) return ''

        //Grab its string representation and process.
        var originUrlString = originUrls.first().toString()
        //Determine the protocol
        if (originUrlString.lastIndexOf(':') > 'https://'.length()) {
            //If ssh then check for authentication data.
            if (originUrlString.contains('@')) {
                //We have authentication data: Strip it.
                return 'https://' + originUrlString.substring(originUrlString.indexOf('@') + 1).replace('.git', '').replace(':', '/')
            } else {
                //No authentication data: Switch to https.
                return 'https://' + originUrlString.replace('ssh://', '').replace('.git', '').replace(':', '/')
            }
        } else if (originUrlString.startsWith('http')) {
            //Standard http protocol: Strip the '.git' ending only.
            return originUrlString.replace('.git', '')
        }

        //What other case exists? Just to be sure lets return this.
        return originUrlString
    }

    private static boolean buildProjectUrlDeprecationLogged
    @Deprecated(forRemoval = true, since = '2.4')
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    private static void buildProjectUrlLogDeprecation() {
        if (!buildProjectUrlDeprecationLogged) {
            println 'WARNING: Usage of GradleUtils.buildProjectUrl is DEPRECATED and will be removed in GradleUtils 3.0! Use gitversion.url instead.'
            buildProjectUrlDeprecationLogged = true
        }
    }

    /**
     * Configures CI related tasks for TeamCity.
     *
     * @param project The project to configure TeamCity tasks for
     * @deprecated Once Forge has completely moved off of TeamCity, this will be deleted. New tasks added to GradleUtils should handle registration themselves.
     */
    @Deprecated(forRemoval = true)
    static void setupCITasks(Project project) {
        project.tasks.register ConfigureTeamCity.NAME, ConfigureTeamCity
    }
}
