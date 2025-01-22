/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider

import javax.inject.Inject

@CompileStatic
class GradleUtilsExtension {
    private final Project project
    private final String defaultFilter
    final DirectoryProperty gitRoot
    private Provider<Map<String, String>> gitInfo

    @Inject
    GradleUtilsExtension(Project project) {
        this.project = project

        this.defaultFilter = GradleUtils.makeFilterFromSubproject(project)
        this.gitRoot = project.objects.directoryProperty().convention(GradleUtils.findGitRoot(project))
        this.gitInfo = project.objects.mapProperty(String, String).convention(project.provider { GradleUtils.gitInfoCheckSubproject(project, this.defaultFilter) })
    }

    /**
     * Returns a version in the form {@code $tag.$offset}, e.g. 1.0.5
     *
     * @return a version in the form {@code $tag.$offset}, e.g. 1.0.5
     */
    String getTagOffsetVersion() {
        return GradleUtils.getTagOffsetVersion(this.getGitInfo())
    }

    String getTagOffsetVersion(String tagPrefix) {
        if (tagPrefix === null || tagPrefix.isEmpty())
            return this.tagOffsetVersion

        if (!tagPrefix.endsWith("-")) tagPrefix += "-"
        return this.getFilteredTagOffsetVersion(true, tagPrefix)
    }

    /**
     * Returns a version in the form {@code $tag.$offset}, e.g. 1.0.5.
     * The provided filter is used to filter the retrieved tag.
     *
     * @param prefix If true, will treat the filter as a prefix.
     * Defaults to false, which means to treat the filter as a glob pattern.
     * @param filter A non-null string filter used when retrieving the tag
     * @return a version in the form {@code $tag.$offset}, e.g. 1.0.5
     */
    String getFilteredTagOffsetVersion(boolean prefix = false, String filter) {
        // I'm really sorry
        // We need to set the gitInfo again because if we call this with a custom tag prefix, it will not update the
        // originally set gitInfo. so this way, we can use the newly-set gitInfo after this method wherever else we
        // might need it
        // version = gradleutils.getTagOffsetVersion('different-tag-prefix')
        var newGitInfo = prefix ? GradleUtils.gitInfoCheckSubproject(project, this.defaultFilter, filter) : GradleUtils.gitInfoCheckSubproject(project, this.defaultFilter, this.defaultFilter, filter);
        this.gitInfo = this.project.provider { newGitInfo }

        return GradleUtils.getTagOffsetVersion(newGitInfo)
    }

    /**
     * Returns a version in the form {@code $tag.$offset}, optionally with the branch
     * appended if it is not in the defined list of allowed branches
     *
     * @param allowedBranches A list of allowed branches; the current branch is appended if not in this list
     * @return a version in the form {@code $tag.$offset} or {@code $tag.$offset-$branch}
     */
    String getTagOffsetBranchVersion(String... allowedBranches) {
        return GradleUtils.getTagOffsetBranchVersion(this.getGitInfo(), allowedBranches)
    }

    String getTagOffsetBranchVersionWithTagPrefix(String tagPrefix, String... allowedBranches) {
        if (tagPrefix === null || tagPrefix.isEmpty())
            return this.getTagOffsetBranchVersion(allowedBranches)

        if (!tagPrefix.endsWith("-")) tagPrefix += "-"
        return this.getFilteredTagOffsetBranchVersion(true, tagPrefix, allowedBranches)
    }

    /**
     * Returns a version in the form {@code $tag.$offset}, optionally with the branch
     * appended if it is not in the defined list of allowed branches.
     * The provided filter is used to filter the retrieved tag.
     *
     * @param prefix If true, will treat the filter as a prefix.
     * Defaults to false, which means to treat the filter as a glob pattern.
     * @param filter A non-null string filter used when retrieving the tag
     * @param allowedBranches A list of allowed branches; the current branch is appended if not in this list
     * @return a version in the form {@code $tag.$offset} or {@code $tag.$offset-$branch}
     */
    String getFilteredTagOffsetBranchVersion(boolean prefix = false, String filter, String... allowedBranches) {
        var newGitInfo = prefix ? GradleUtils.gitInfoCheckSubproject(project, filter) : GradleUtils.gitInfoCheckSubproject(project, this.defaultFilter, filter);
        this.gitInfo = this.project.provider { newGitInfo }

        return GradleUtils.getFilteredTagOffsetBranchVersion(newGitInfo, prefix, filter, allowedBranches)
    }

    /**
     * Returns a version in the form {@code $mcVersion-$tag.$offset}, optionally with
     * the branch appended if it is not in the defined list of allowed branches
     *
     * @param mcVersion The current minecraft version
     * @param allowedBranches A list of allowed branches; the current branch is appended if not in this list
     * @return a version in the form {@code $mcVersion-$tag.$offset} or {@code $mcVersion-$tag.$offset-$branch}
     */
    String getMCTagOffsetBranchVersion(String mcVersion, String... allowedBranches) {
        return GradleUtils.getMCTagOffsetBranchVersion(getGitInfo(), mcVersion, allowedBranches)
    }

    String getMCTagOffsetBranchVersionWithTagPrefix(String tagPrefix, String mcVersion, String... allowedBranches) {
        if (tagPrefix === null || tagPrefix.isEmpty())
            return this.getMCTagOffsetBranchVersion(mcVersion, allowedBranches)

        if (!tagPrefix.endsWith("-")) tagPrefix += "-"
        return this.getFilteredMCTagOffsetBranchVersion(true, tagPrefix, mcVersion, allowedBranches)
    }

    /**
     * Returns a version in the form {@code $mcVersion-$tag.$offset}, optionally with
     * the branch appended if it is not in the defined list of allowed branches.
     * The provided filter is used to filter the retrieved tag.
     *
     * @param prefix If true, will treat the filter as a prefix.
     * Defaults to false, which means to treat the filter as a glob pattern.
     * @param filter A non-null string filter used when retrieving the tag
     * @param mcVersion The current minecraft version
     * @param allowedBranches A list of allowed branches; the current branch is appended if not in this list
     * @return a version in the form {@code $mcVersion-$tag.$offset} or {@code $mcVersion-$tag.$offset-$branch}
     */
    String getFilteredMCTagOffsetBranchVersion(boolean prefix = false, String filter, String mcVersion, String... allowedBranches) {
        var newGitInfo = prefix ? GradleUtils.gitInfoCheckSubproject(project, filter) : GradleUtils.gitInfoCheckSubproject(project, this.defaultFilter, filter);
        this.gitInfo = this.project.provider { newGitInfo }

        return GradleUtils.getFilteredMCTagOffsetBranchVersion(newGitInfo, prefix, filter, mcVersion, allowedBranches)
    }

    Map<String, String> getGitInfo() {
        return this.gitInfo.get()
    }

    /**
     * Get a closure to be passed into {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(groovy.lang.Closure)}
     * in a publishing block.
     *
     * @param defaultFolder The default folder if the required maven information is not currently set
     * @return a closure
     */
    Closure getPublishingForgeMaven(File defaultFolder = project.rootProject.file('repo')) {
        return GradleUtils.getPublishingForgeMaven(project, defaultFolder)
    }

    /**
     * Get a closure for the Forge maven to be passed into {@link org.gradle.api.artifacts.dsl.RepositoryHandler#maven(groovy.lang.Closure)}
     * in a repositories block.
     *
     * @return a closure
     */
    static Closure getForgeMaven() {
        return GradleUtils.getForgeMaven()
    }

    static Closure getMinecraftLibsMaven() {
        return GradleUtils.getMinecraftLibsMaven()
    }

    /**
     * Gets an instance of {@link PomUtils} allowing the groovy script to not need to import the PomUtils class.
     */
    static PomUtils getPom() {
        return POM_INSTANCE
    }
    private static final PomUtils POM_INSTANCE = new PomUtils()
}
