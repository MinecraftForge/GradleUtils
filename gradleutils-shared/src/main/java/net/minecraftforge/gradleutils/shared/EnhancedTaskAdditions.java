/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;

sealed interface EnhancedTaskAdditions extends EnhancedPluginAdditions permits EnhancedTask, ToolExecSpec {
    EnhancedPlugin<?> plugin();

    String baseName();

    default Tool.Resolved getTool(Tool tool) {
        return this.plugin().getTool(tool);
    }

    default DirectoryProperty globalCaches() {
        return this.plugin().globalCaches();
    }

    default DirectoryProperty localCaches() {
        return this.plugin().localCaches();
    }

    default DirectoryProperty rootProjectDirectory() {
        return this.plugin().rootProjectDirectory();
    }

    default DirectoryProperty workingProjectDirectory() {
        return this.plugin().workingProjectDirectory();
    }

    /// The default output directory to use for this task if it outputs a directory.
    ///
    /// @return A provider for the directory
    default @Internal Provider<Directory> getDefaultOutputDirectory() {
        return this.localCaches().dir(this.baseName()).map(this.plugin().getProblemsInternal().ensureFileLocation());
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
    default @Internal Provider<RegularFile> getDefaultOutputFile(String ext) {
        return this.getOutputFile("output." + ext);
    }

    /// The default output log file to use for this task.
    ///
    /// @return A provider for the file
    default @Internal Provider<RegularFile> getDefaultLogFile() {
        return this.getOutputFile("log.txt");
    }

    /// A file with the specified name in the default output directory.
    ///
    /// @param fileName The name of the output file
    /// @return A provider for the file
    default @Internal Provider<RegularFile> getOutputFile(String fileName) {
        return this.localCaches().file(String.format("%s/%s", this.baseName(), fileName)).map(this.plugin().getProblemsInternal().ensureFileLocation());
    }
}
