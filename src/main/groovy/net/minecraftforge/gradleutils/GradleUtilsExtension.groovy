/*
 * GradleUtils
 * Copyright (C) 2021 Forge Development LLC
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package net.minecraftforge.gradleutils

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider

import javax.inject.Inject

@CompileStatic
class GradleUtilsExtension {
    private final Project project
    final DirectoryProperty gitRoot
    private final Provider<Map<String, String>> gitInfo

    @Inject
    GradleUtilsExtension(Project project) {
        this.project = project

        this.gitRoot = project.objects.directoryProperty().convention(project.layout.projectDirectory)
        this.gitInfo = project.objects.mapProperty(String, String)
                .convention(gitRoot.map((Directory dir) -> GradleUtils.gitInfo(dir.asFile)))
    }

    /**
     * Returns a version in the form {@code $tag.$offset}, e.g. 1.0.5
     *
     * @return a version in the form {@code $tag.$offset}, e.g. 1.0.5
     */
    String getTagOffsetVersion() {
        return GradleUtils.getTagOffsetVersion(getGitInfo())
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
        return GradleUtils.getFilteredTagOffsetBranchVersion(getGitInfo(), prefix, filter)
    }

    /**
     * Returns a version in the form {@code $tag.$offset}, optionally with the branch
     * appended if it is not in the defined list of allowed branches
     *
     * @param allowedBranches A list of allowed branches; the current branch is appended if not in this list
     * @return a version in the form {@code $tag.$offset} or {@code $tag.$offset-$branch}
     */
    String getTagOffsetBranchVersion(String... allowedBranches) {
        return GradleUtils.getTagOffsetBranchVersion(getGitInfo(), allowedBranches)
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
        return GradleUtils.getFilteredTagOffsetBranchVersion(getGitInfo(), prefix, filter, allowedBranches)
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
        return GradleUtils.getFilteredMCTagOffsetBranchVersion(getGitInfo(), prefix, filter, mcVersion, allowedBranches)
    }

    Map<String, String> getGitInfo() {
        return gitInfo.get()
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
}
