/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.Action;
import org.gradle.api.plugins.ExtensionAware;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public interface ToolsExtension extends ExtensionAware {
    void configure(String name, Action<? super Tool.Definition> action);
}
