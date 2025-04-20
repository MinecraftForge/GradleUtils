/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.changelog;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;

abstract class ChangelogPlugin implements Plugin<Project> {
    @Inject
    public ChangelogPlugin() { }

    @Override
    public void apply(Project project) {
        project.getExtensions().add(ChangelogExtension.class, ChangelogExtension.NAME, new ChangelogExtensionImpl(
            project,
            this.getObjects()
        ));
    }

    protected abstract @Inject ObjectFactory getObjects();
}
