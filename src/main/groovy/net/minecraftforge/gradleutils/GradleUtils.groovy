/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.json.JsonOutput
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.RemoteConfig
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.authentication.http.BasicAuthentication

import javax.annotation.Nullable

@CompileStatic
class GradleUtils {

    @CompileDynamic
    private static void initDynamic() {
        String.metaClass.rsplit = GradleUtils.&rsplit
    }

    @Nullable
    static List<String> rsplit(@Nullable String input, String del, int limit = -1) {
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

    static Map<String, String> gitInfo(File dir, String... globFilters) {
        var git
        try {
            git = Git.open(dir)
        } catch (RepositoryNotFoundException e) {
            return [
                tag: '0.0',
                offset: '0',
                hash: '00000000',
                branch: 'master',
                commit: '0000000000000000000000',
                abbreviatedId: '00000000'
            ]
        }
        String tag = git.describe().setLong(true).setTags(true).setMatch(globFilters ?: new String[0]).call()
        List<String> desc = rsplit(tag, '-', 2) ?: ['0.0', '0', '00000000']
        Ref head = git.repository.exactRef('HEAD')
        String longBranch = head.symbolic ? head?.target?.name : null // matches Repository.getFullBranch() but returning null when on a detached HEAD

        Map<String, String> ret = [:]
        ret.dir = dir.absolutePath
        ret.tag = desc[0]
        if (ret.tag.startsWith("v") && ret.tag.length() > 1 && ret.tag.charAt(1).digit)
            ret.tag = ret.tag.substring(1)
        ret.offset = desc[1]
        ret.hash = desc[2]
        ret.branch = longBranch !== null ? Repository.shortenRefName(longBranch) : null
        ret.commit = ObjectId.toString(head.objectId)
        ret.abbreviatedId = head.objectId.abbreviate(8).name()

        // Remove any lingering null values
        ret.removeAll {it.value === null }

        return ret
    }

    /**
     * Get a closure to be passed into {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(groovy.lang.Closure)}
     * in a publishing block.
     *
     * Important the following environment variables must be set for this to work:
     *  - MAVEN_USER: Containing the username to use for authentication
     *  - MAVEN_PASSWORD: Containing the password to use for authentication
     *  - MAVEN_URL_RELEASE: Containing the URL to use for the release repository
     *  - MAVEN_URL_SNAPSHOT: Containing the URL to use for the snapshot repository
     *
     * @param project The project
     * @param defaultFolder The default folder if the required maven information is not currently set
     * @return a closure
     */
    static Closure getPublishingForgeMaven(Project project, File defaultFolder = project.rootProject.file('repo')) {
        return setupSnapshotCompatiblePublishing(project, 'https://maven.minecraftforge.net/', defaultFolder)
    }

    /**
     * Get a closure to be passed into {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(groovy.lang.Closure)}
     * in a publishing block, this closure respects the current project's version, with regards to publishing to a release
     * or snapshot repository.
     *
     * Important the following environment variables must be set for this to work:
     *  - MAVEN_USER: Containing the username to use for authentication
     *  - MAVEN_PASSWORD: Containing the password to use for authentication
     *
     * The following environment variables are optional:
     *  - MAVEN_URL_RELEASE: Containing the URL to use for the release repository
     *  - MAVEN_URL_SNAPSHOT: Containing the URL to use for the snapshot repository
     *
     * If the MAVEN_URL_RELEASE is not set the passed in fallback URL will be used for the release repository.
     * By default this is: https://maven.minecraftforge.net/
     * This is done to preserve backwards compatibility with the old {@link #getPublishingForgeMaven(Project, File)} method.
     *
     * @param project The project
     * @param defaultFolder The default folder if the required maven information is not currently set
     * @return a closure
     */
    static Closure setupSnapshotCompatiblePublishing(Project project, String fallbackPublishingEndpoint = 'https://maven.minecraftforge.net/', File defaultFolder = project.rootProject.file('repo'), File defaultSnapshotFolder = project.rootProject.file('snapshots')) {
        return { MavenArtifactRepository repo ->
            repo.name = 'forge'

            if (System.getenv('MAVEN_USER') && System.getenv('MAVEN_PASSWORD')) {
                String publishingEndpoint = fallbackPublishingEndpoint
                if (System.getenv('MAVEN_URL_RELEASE')) {
                    publishingEndpoint = System.getenv('MAVEN_URL_RELEASE')
                }

                if (project.version.toString().endsWith('-SNAPSHOT') && System.getenv('MAVEN_URL_SNAPSHOTS')) {
                    repo.url = System.getenv('MAVEN_URL_SNAPSHOTS')
                } else {
                    repo.url = publishingEndpoint
                }
                repo.authentication { auth ->
                    auth.create('basic', BasicAuthentication)
                }
                repo.credentials { creds ->
                    creds.username = System.getenv('MAVEN_USER')
                    creds.password = System.getenv('MAVEN_PASSWORD')
                }
            } else {
                if (project.version.toString().endsWith('-SNAPSHOT')) {
                    repo.url = 'file://' + defaultSnapshotFolder.absolutePath
                } else {
                    repo.url = 'file://' + defaultFolder.absolutePath
                }
            }
        }
    }

    /**
     * Get a closure for the Forge maven to be passed into {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(groovy.lang.Closure)}
     * in a repositories block.
     */
    static Closure getForgeMaven() {
        return { MavenArtifactRepository repo ->
            repo.name = 'MinecraftForge'
            repo.url = 'https://maven.minecraftforge.net/'
        }
    }

    /**
     * Get a closure for the Forge maven to be passed into {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(groovy.lang.Closure)}
     * in a repositories block.
     */
    static Closure getForgeReleaseMaven() {
        return { MavenArtifactRepository repo ->
            repo.name = 'MinecraftForge releases'
            repo.url = 'https://maven.minecraftforge.net/releases'
        }
    }

    /**
     * Get a closure for the Forge maven to be passed into {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(groovy.lang.Closure)}
     * in a repositories block.
     */
    static Closure getForgeSnapshotMaven() {
        return { MavenArtifactRepository repo ->
            repo.name = 'MinecraftForge snapshots'
            repo.url = 'https://maven.minecraftforge.net/snapshots'
        }
    }

    /**
     * Get a closure for the Minecraft libraries maven to be passed into {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(groovy.lang.Closure)}
     * in a repositories block.
     */
    static Closure getMinecraftLibsMaven() {
        return { MavenArtifactRepository repo ->
            repo.name = 'Minecraft libraries'
            repo.url = 'https://libraries.minecraft.net/'
        }
    }

    private static Map<String, String> getFilteredInfo(Map<String, String> info, boolean prefix, String filter) {
        if (prefix)
            filter += '**'
        return gitInfo(new File(info.dir), filter)
    }

    /**
     * Returns a version in the form {@code $tag.$offset}, e.g. 1.0.5
     *
     * @param info A git info object generated from {@link #gitInfo}
     * @return a version in the form {@code $tag.$offset}, e.g. 1.0.5
     */
    static String getTagOffsetVersion(Map<String, String> info) {
        return "${info.tag}.${info.offset}"
    }

    /**
     * Returns a version in the form {@code $tag.$offset}, e.g. 1.0.5.
     * The provided filter is used to filter the retrieved tag.
     *
     * @param info A git info object generated from {@link #gitInfo}
     * @param prefix If true, will treat the filter as a prefix.
     * Defaults to false, which means to treat the filter as a glob pattern.
     * @param filter A non-null string filter used when retrieving the tag
     * @return a version in the form {@code $tag.$offset}, e.g. 1.0.5
     */
    static String getFilteredTagOffsetVersion(Map<String, String> info, boolean prefix = false, String filter) {
        return getTagOffsetVersion(getFilteredInfo(info, prefix, filter))
    }

    /**
     * Returns a version in the form {@code $tag.$offset}, optionally with the branch
     * appended if it is not in the defined list of allowed branches
     *
     * @param info A git info object generated from {@link #gitInfo}
     * @param allowedBranches A list of allowed branches; the current branch is appended if not in this list
     * @return a version in the form {@code $tag.$offset} or {@code $tag.$offset-$branch}
     */
    static String getTagOffsetBranchVersion(Map<String, String> info, String... allowedBranches) {
        if (!allowedBranches || allowedBranches.length === 0)
            allowedBranches = [null, 'master', 'main', 'HEAD']
        final version = getTagOffsetVersion(info)
        String branch = info.branch
        if (branch?.startsWith('pulls/'))
            branch = 'pr' + rsplit(branch, '/', 1)[1]
        branch = branch?.replaceAll(/[\\\/]/, '-')
        return branch in allowedBranches ? version : "$version-${branch}"
    }

    /**
     * Returns a version in the form {@code $tag.$offset}, optionally with the branch
     * appended if it is not in the defined list of allowed branches.
     * The provided filter is used to filter the retrieved tag.
     *
     * @param info A git info object generated from {@link #gitInfo}
     * @param prefix If true, will treat the filter as a prefix.
     * Defaults to false, which means to treat the filter as a glob pattern.
     * @param filter A non-null string filter used when retrieving the tag
     * @param allowedBranches A list of allowed branches; the current branch is appended if not in this list
     * @return a version in the form {@code $tag.$offset} or {@code $tag.$offset-$branch}
     */
    static String getFilteredTagOffsetBranchVersion(Map<String, String> info, boolean prefix = false, String filter, String... allowedBranches) {
        return getTagOffsetBranchVersion(getFilteredInfo(info, prefix, filter), allowedBranches)
    }

    /**
     * Returns a version in the form {@code $mcVersion-$tag.$offset}, optionally with
     * the branch appended if it is not in the defined list of allowed branches
     *
     * @param info A git info object generated from {@link #gitInfo}
     * @param mcVersion The current minecraft version
     * @param allowedBranches A list of allowed branches; the current branch is appended if not in this list
     * @return a version in the form {@code $mcVersion-$tag.$offset} or {@code $mcVersion-$tag.$offset-$branch}
     */
    static String getMCTagOffsetBranchVersion(Map<String, String> info, String mcVersion, String... allowedBranches) {
        if (!allowedBranches || allowedBranches.length === 0)
            allowedBranches = [null, 'master', 'main', 'HEAD', mcVersion, mcVersion + '.0', mcVersion + '.x', rsplit(mcVersion, '.', 1)[0] + '.x']
        return "$mcVersion-${getTagOffsetBranchVersion(info, allowedBranches)}"
    }

    /**
     * Returns a version in the form {@code $mcVersion-$tag.$offset}, optionally with
     * the branch appended if it is not in the defined list of allowed branches.
     * The provided filter is used to filter the retrieved tag.
     *
     * @param info A git info object generated from {@link #gitInfo}
     * @param prefix If true, will treat the filter as a prefix.
     * Defaults to false, which means to treat the filter as a glob pattern.
     * @param filter A non-null string filter used when retrieving the tag
     * @param mcVersion The current minecraft version
     * @param allowedBranches A list of allowed branches; the current branch is appended if not in this list
     * @return a version in the form {@code $mcVersion-$tag.$offset} or {@code $mcVersion-$tag.$offset-$branch}
     */
    static String getFilteredMCTagOffsetBranchVersion(Map<String, String> info, boolean prefix = false, String filter, String mcVersion, String... allowedBranches) {
        return getMCTagOffsetBranchVersion(getFilteredInfo(info, prefix, filter), mcVersion, allowedBranches)
    }

    /**
     * Builds a project url for a project under the minecraft forge organisation.
     *
     * @param project The name of the project. (As in the project slug on github).
     * @return The github url of the project.
     */
    static String buildProjectUrl(String project) {
        return buildProjectUrl("MinecraftForge", project)
    }

    /**
     * Builds a project url for a project under the given organisation.
     *
     * @param organisation The name of the org. (As in the org slug on github).
     * @param project The name of the project. (As in the project slug on github).
     * @return The github url of the project.
     */
    static String buildProjectUrl(String organisation, String project) {
        return "https://github.com/$organisation/$project"
    }

    /**
     * Builds the github url from the origin remotes push uri.
     * Processes the URI from three different variants into the URL:
     * 1) If the protocol is http(s) based then ".git" is stripped and returned as url.
     * 2) If the protocol is ssh and does contain authentication information then the
     *    username and password are stripped and the url is returned without the ".git"
     *    ending.
     * 3) If the protocol is ssh and does not contain authentication information then
     *    the protocol is switched to https and the ".git" ending is stripped.
     *
     * @param projectDir THe project directory.
     * @return
     */
    static String buildProjectUrl(Git git) {
        List<RemoteConfig> remotes = git.remoteList().call() //Get all remotes.
        if (remotes.isEmpty())
            throw new IllegalStateException("No remotes found in " + git.repository.directory)

        //Get the origin remote.
        final originRemote = remotes.find { remote -> remote.name == 'origin' }

        //We do not have an origin named remote
        if (originRemote === null)
            return ''

        //Get the origin push url.
        final originUrl = originRemote.URIs.first()

        //We do not have a origin url
        if (originUrl === null)
            return ''

        //Grab its string representation and process.
        final originUrlString = originUrl.toString()
        //Determine the protocol
        if (originUrlString.startsWith("ssh")) {
            //If ssh then check for authentication data.
            if (originUrlString.contains("@")) {
                //We have authentication data: Strip it.
                return "https://" + originUrlString.substring(originUrlString.indexOf("@") + 1).replace(".git", "")
            } else {
                //No authentication data: Switch to https.
                return "https://" + originUrlString.substring(6).replace(".git", "")
            }
        } else if (originUrlString.startsWith("http")) {
            //Standard http protocol: Strip the ".git" ending only.
            return originUrlString.replace(".git", "")
        }

        //What other case exists? Just to be sure lets return this.
        return originUrlString
    }

    /**
     * Configures CI related tasks for all known platforms.
     *
     * @param project The project to configure them on.
     */
    static void setupCITasks(Project project) {
        setupTeamCityTasks(project)
        GitHubActions.setupTasks(project)
    }

    /**
     * Sets up the TeamCity CI tasks.
     *
     * @param project The project to configure it on.
     */
    @CompileDynamic
    private static void setupTeamCityTasks(Project project) {
        if (System.getenv('TEAMCITY_VERSION')) {
            //Only setup the CI environment if and only if the environment variables are set.
            def teamCityCITask = project.tasks.register("configureTeamCity") {
                //Print the marker lines into the log which configure the pipeline.
                doLast {
                    project.logger.lifecycle("Setting project variables and parameters.")
                    println "##teamcity[buildNumber '${project.version}']"
                    println "##teamcity[setParameter name='env.PUBLISHED_JAVA_ARTIFACT_VERSION' value='${project.version}']"
                }
            }
        }
    }

    private static class GitHubActions {
        private static void setupTasks(final Project project) {
            // Setup the GitHub Actions project info task
            project.tasks.register('ghActionsProjectInfoJson') { Task task ->
                task.onlyIf { System.getenv('GITHUB_ENV') !== null }
                task.doLast {
                    project.file(System.getenv('GITHUB_ENV')) << "\nPROJ_INFO_JSON=" + JsonOutput.toJson(getProjectInfo(project))
                }
            }

            project.tasks.register('ghActionsProjectVersion') { Task task ->
                task.onlyIf { System.getenv('GITHUB_ENV') !== null }
                task.doLast {
                    project.file(System.getenv('GITHUB_ENV')) << "\nPROJ_VERSION=${project.version}"
                }
            }
        }

        @Immutable
        private static final class ProjectInfo {
            String group = ''
            String name = ''
            String version = ''
        }

        private static List<ProjectInfo> getProjectInfo(final Project project) {
            return project.allprojects.collect { Project proj ->
                new ProjectInfo(project.group.toString(), proj.name, proj.version.toString())
            }
        }
    }
}
