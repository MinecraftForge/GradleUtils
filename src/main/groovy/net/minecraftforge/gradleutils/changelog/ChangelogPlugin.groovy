/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.changelog

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class ChangelogPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create(ChangelogExtension.NAME, ChangelogExtension, project)
    }
}
