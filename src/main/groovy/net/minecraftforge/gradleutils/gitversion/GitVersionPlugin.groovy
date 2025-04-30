/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.gitversion

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory

import javax.inject.Inject

/** The entry point for the Git Version Gradle plugin. Exists to create the {@linkplain GitVersionExtension extension}. */
@CompileStatic
abstract class GitVersionPlugin implements Plugin<Project> {
    /** @see <a href="https://docs.gradle.org/current/userguide/service_injection.html#objectfactory">ObjectFactory Service Injection</a> */
    @Inject abstract ObjectFactory getObjects()
    /** @see <a href="https://docs.gradle.org/current/userguide/service_injection.html#sec:projectlayout">ProjectLayout Service Injection</a> */
    @Inject abstract ProjectLayout getLayout()
    /** @see <a href="https://docs.gradle.org/current/userguide/service_injection.html#providerfactory">ProviderFactory Service Injection</a> */
    @Inject abstract ProviderFactory getProviders()

    @Override
    void apply(Project project) {
        // TODO [GradleUtils][3.0][GitVersion] Use direct constructor
        project.extensions.create(GitVersionExtension.NAME, GitVersionExtension, project, this.objects, this.layout, this.providers)
    }
}
