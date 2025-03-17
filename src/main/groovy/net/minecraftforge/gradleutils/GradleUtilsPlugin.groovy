/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileStatic
import net.minecraftforge.gradleutils.changelog.ChangelogPlugin
import net.minecraftforge.gradleutils.gitversion.GitVersionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory

import javax.inject.Inject

/** The entry point for the Gradle Utils plugin. Exists to create the {@linkplain GradleUtilsExtension extension}. */
@CompileStatic
abstract class GradleUtilsPlugin implements Plugin<Project> {
    @Inject
    abstract ProviderFactory getProviders()

    @Inject
    abstract ObjectFactory getObjects()

    @Override
    void apply(Project project) {
        project.plugins.apply(GitVersionPlugin)
        project.plugins.apply(ChangelogPlugin)
        project.extensions.create(GradleUtilsExtension.NAME, GradleUtilsExtension, project, this.providers, this.objects)
    }
}
