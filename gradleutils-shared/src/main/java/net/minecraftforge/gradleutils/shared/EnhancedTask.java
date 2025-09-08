/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;

/// The enhanced task contains a handful of helper methods to make working with the enhanced plugin and caches easier.
///
/// @param <P> The type of enhanced problems
public non-sealed interface EnhancedTask<P extends EnhancedProblems> extends Task, EnhancedPluginAdditions {
    /// The enhanced plugin type for this task.
    ///
    /// @return The plugin type
    Class<? extends EnhancedPlugin<? super Project>> pluginType();

    /// The enhanced problems type for this task.
    ///
    /// @return The problems type
    Class<P> problemsType();

    private EnhancedPlugin<? super Project> getPlugin() {
        return this.getProject().getPlugins().getPlugin(this.pluginType());
    }

    @Override
    default Tool.Resolved getTool(Tool tool) {
        return this.getPlugin().getTool(tool);
    }

    @Override
    default DirectoryProperty globalCaches() {
        return this.getPlugin().globalCaches();
    }

    @Override
    default DirectoryProperty localCaches() {
        return this.getPlugin().localCaches();
    }

    @Override
    default DirectoryProperty rootProjectDirectory() {
        return this.getPlugin().rootProjectDirectory();
    }

    @Override
    default DirectoryProperty workingProjectDirectory() {
        return this.getPlugin().workingProjectDirectory();
    }

    /// The default output directory to use for this task if it outputs a directory.
    ///
    /// @return A provider for the directory
    default @Internal Provider<Directory> getDefaultOutputDirectory() {
        return this.localCaches().dir(this.getName()).map(this.getPlugin().getProblemsInternal().ensureFileLocation());
    }

    /// The default output file to use for this task if it outputs a file. Uses the `.jar` extension.
    ///
    /// @return A provider for the file
    default @Internal Provider<RegularFile> getDefaultOutputFile() {
        return this.getDefaultOutputFile("jar");
    }

    /// The default output file to use for this task if it outputs a file.
    ///
    /// @param ext The extension to use for the file
    /// @return A provider for the file
    default Provider<RegularFile> getDefaultOutputFile(String ext) {
        return this.localCaches().file(String.format("%s/output.%s", this.getName(), ext)).map(this.getPlugin().getProblemsInternal().ensureFileLocation());
    }
}
