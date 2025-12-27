/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.internal;

import net.minecraftforge.gradleutils.shared.EnhancedPlugin;
import net.minecraftforge.gradleutils.shared.EnhancedTask;
import org.gradle.api.Project;

interface GradleUtilsTask extends EnhancedTask<GradleUtilsProblems> {
    @Override
    default Class<? extends EnhancedPlugin<? super Project>> pluginType() {
        return GradleUtilsPlugin.class;
    }

    @Override
    default Class<GradleUtilsProblems> problemsType() {
        return GradleUtilsProblems.class;
    }
}
