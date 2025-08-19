package net.minecraftforge.gradleutils.shared;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Provider;

import java.io.File;

/// This interface defines the additional methods added by [EnhancedPlugin]. They are additionally accessible in tasks
/// that implement [EnhancedTask].
public interface EnhancedPluginAdditions {
    /// Gets a provider to the file for a [Tool] to be used. The tool's state is managed by Gradle through the
    /// [org.gradle.api.provider.ValueSource] API and will not cause caching issues.
    ///
    /// @param tool The tool to get
    /// @return A provider for the tool file
    Provider<File> getTool(Tool tool);

    /// Gets the global caches to be used for this plugin. These caches persist between projects and should be used to
    /// eliminate excess work done by projects that request the same data.
    ///
    /// It is stored in `~/.gradle/caches/minecraftforge/plugin`.
    ///
    /// @return The global caches
    /// @throws RuntimeException If this plugin cannot access global caches (i.e. the target is not
    ///                          [org.gradle.api.Project] or [org.gradle.api.initialization.Settings])
    DirectoryProperty globalCaches();

    /// Gets the local caches to be used for this plugin. Data done by tasks that should not be shared between projects
    /// should be stored here.
    ///
    /// It is located in `project/build/minecraftforge/plugin`.
    ///
    /// @return The global caches
    /// @throws RuntimeException If this plugin cannot access global caches (i.e. the target is not
    ///                          [org.gradle.api.Project] or [org.gradle.api.initialization.Settings])
    DirectoryProperty localCaches();
}
