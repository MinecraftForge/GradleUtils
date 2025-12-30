/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import org.gradle.api.Task;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.jetbrains.annotations.ApiStatus;

/// This task generates the GitHub Actions workflow file for the project.
///
/// If the project is also using the Git Version plugin (currently auto-applied by GradleUtils), it will respect any
/// declared subprojects.
@ApiStatus.Internal
public interface GenerateActionsWorkflow extends Task {
    /// The name for this task.
    ///
    /// Each [project][org.gradle.api.Project] should only have one of this type of task and it must be named this.
    String NAME = "generateActionsWorkflow";

    //@formatter:off -- newline breaks the formatting for the default location in JavaDoc
    /**
     * The output file for this task.
     * <p>Default:
     * {@link org.gradle.api.Project#getRootProject() rootProject}{@code /.github/workflows/publish_}{@link org.gradle.api.Project#getName() name}{@code .yaml}</p>
     *
     * @return The property for the output file
     */
    @OutputFile RegularFileProperty getOutputFile();
    //@formatter:on

    /// The project name to use in the workflow file.
    ///
    /// Default: [org.gradle.api.Project#getName()]
    ///
    /// @return The property for the project name
    @Input Property<String> getProjectName();

    /// The branch name to use in the workflow file.
    ///
    /// Default: Automatically detected by Git Version, otherwise `master`
    ///
    /// @return The property for the branch name
    @Optional @Input Property<String> getBranch();

    /// The local path from the [root project][org.gradle.api.Project#getRootProject()] to the current project to use in
    /// the workflow file. This local path is not used to invoke Gradle but rather for Git Version.
    ///
    /// @return The property for the local path
    @Optional @Input Property<String> getLocalPath();

    /// The paths to pass into the workflow file. Prepend a path with `!` to ignore it instead of include it. The Git
    /// Version plugin will automatically add declared subproject paths to exclude.
    ///
    /// @return The property for the paths
    @Optional @Input ListProperty<String> getPaths();

    /// The Java version to invoke Gradle with.
    ///
    /// Default: The project's toolchain version, or `17` if it is lower than that.
    ///
    /// @return The property for the Gradle Java version
    @Input Property<Integer> getGradleJavaVersion();

    /// The Shared Actions branch to use with this workflow.
    ///
    /// Default: `v0`
    ///
    /// @return The property for the Shared Actions branch
    @Input Property<String> getSharedActionsBranch();
}
