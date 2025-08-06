/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import net.minecraftforge.gradleutils.shared.EnhancedPlugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.ExtensionAware;

import javax.inject.Inject;

abstract class GradleUtilsPlugin extends EnhancedPlugin<ExtensionAware> {
    static final String NAME = "gradleutils";
    static final String DISPLAY_NAME = "MinecraftForge Gradle Utilities";

    static final Logger LOGGER = Logging.getLogger(GradleUtilsPlugin.class);

    @Inject
    public GradleUtilsPlugin() {
        super(NAME, DISPLAY_NAME);
    }

    @Override
    public void setup(ExtensionAware target) {
        if (target instanceof Project project)
            project.getExtensions().create(GradleUtilsExtension.NAME, GradleUtilsExtensionImpl.ForProjectImpl.class, project);
        else
            target.getExtensions().create(GradleUtilsExtension.NAME, GradleUtilsExtensionImpl.class);
    }
}
