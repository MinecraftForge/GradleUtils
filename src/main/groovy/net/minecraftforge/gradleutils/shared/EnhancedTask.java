/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;

/// The enhanced task contains a handful of helper methods to make working with the enhanced plugin and caches easier.
///
/// @param <T> The type of enhanced plugin
public interface EnhancedTask<T extends EnhancedPlugin<? super Project>> extends Task {
    /// The enhanced plugin type for this task.
    ///
    /// @return The plugin type
    @Internal
    Class<T> getPluginType();

    /// The enhanced plugin associated with this task.
    ///
    /// @return The plugin
    @Internal
    default T getPlugin() {
        return this.getProject().getPlugins().getPlugin(this.getPluginType());
    }

    /// The default output directory to use for this task if it outputs a directory.
    ///
    /// @return A provider for the directory
    @Internal
    default Provider<Directory> getDefaultOutputDirectory() {
        return this.getPlugin().getLocalCaches().dir(this.getName()).map(this.getPlugin().getProblemsInternal().ensureFileLocation());
    }

    /// The default output file to use for this task if it outputs a file. Uses the `.jar` extension.
    ///
    /// @return A provider for the file
    @Internal
    default Provider<RegularFile> getDefaultOutputFile() {
        return this.getDefaultOutputFile("jar");
    }

    /// The default output file to use for this task if it outputs a file.
    ///
    /// @param ext The extension to use for the file
    /// @return A provider for the file
    @Internal
    default Provider<RegularFile> getDefaultOutputFile(String ext) {
        return this.getPlugin().getLocalCaches().file("%s/output.%s".formatted(this.getName(), ext)).map(this.getPlugin().getProblemsInternal().ensureFileLocation());
    }
}
