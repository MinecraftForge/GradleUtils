/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.Action;
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
public non-sealed interface EnhancedTask<P extends EnhancedProblems> extends Task, EnhancedPluginAdditions, EnhancedTaskAdditions {
    /// The enhanced plugin type for this task.
    ///
    /// @return The plugin type
    Class<? extends EnhancedPlugin<? super Project>> pluginType();

    /// The enhanced problems type for this task.
    ///
    /// @return The problems type
    Class<P> problemsType();

    @Override
    default EnhancedPlugin<? super Project> plugin() {
        return this.getProject().getPlugins().getPlugin(this.pluginType());
    }

    @Override
    default String baseName() {
        return this.getName();
    }

    default void afterEvaluate(Action<? super Project> action) {
        try {
            getProject().afterEvaluate(action);
        } catch (Exception ignored) {
            action.execute(getProject());
        }
    }
}
