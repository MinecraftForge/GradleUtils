/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.Action;
import org.gradle.api.plugins.ExtensionAware;
import org.jetbrains.annotations.ApiStatus;

/// This extension can be optionally enabled by implementing plugins to allow buildscript authors to drop-in replace, or
/// otherwise customize, the tools that are used by this plugin.
@ApiStatus.Experimental
public sealed interface ToolsExtension extends ExtensionAware permits ToolsExtensionInternal {
    /// Configures the definition of the given tool by its name.
    ///
    /// @param name   The name of the tool to configure the definition for
    /// @param action The configuring action
    void configure(String name, Action<? super Tool.Definition> action);
}
