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

import java.io.File;

/// The enhanced task contains a handful of helper methods to make working with the enhanced plugin and caches easier.
public interface EnhancedTask extends Task, EnhancedPluginAdditions {
    /// The enhanced plugin type for this task.
    ///
    /// @return The plugin type
    Class<? extends EnhancedPlugin<? super Project>> pluginType();

    /// Gets the enhanced plugin used by this task as defined in [#pluginType()].
    ///
    /// @return The enhanced plugin
    /// @deprecated This method is public only due to the limitations imposed by Java 8. Do not use this. Use
    /// [Task#getProject()] -> [org.gradle.api.plugins.PluginAware#getPlugins()] ->
    /// [org.gradle.api.plugins.PluginContainer#getPlugin(Class)].
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    default @Internal EnhancedPlugin<? super Project> getPlugin() {
        return this.getProject().getPlugins().getPlugin(this.pluginType());
    }

    @Override
    default Provider<File> getTool(Tool tool) {
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
