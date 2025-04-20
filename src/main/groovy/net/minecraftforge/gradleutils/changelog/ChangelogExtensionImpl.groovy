/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.changelog

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.annotations.Nullable

/** The heart of the Changelog plugin. This extension is used to enable and partially configure the changelog generation task. */
@CompileStatic
@PackageScope([PackageScopeTarget.CLASS, PackageScopeTarget.CONSTRUCTORS, PackageScopeTarget.FIELDS])
final class ChangelogExtensionImpl implements ChangelogExtension {
    public static final String NAME = 'changelog'

    final Project project

    private final Property<Boolean> publishingAll
    private final Property<Boolean> isGenerating

    private @Lazy TaskProvider<GenerateChangelog> task = {
        this.isGenerating.set true
        this.project.afterEvaluate { project ->
            if (this.publishAll)
                ChangelogUtils.setupChangelogGenerationOnAllPublishTasks project
        }

        ChangelogUtils.setupChangelogTask this.project
    }()

    ChangelogExtensionImpl(Project project, ObjectFactory objects) {
        this.project = project

        this.publishingAll = objects.property(Boolean).convention false
        this.isGenerating = objects.property(Boolean).convention false
    }

    @Override
    void from(@Nullable String marker) {
        this.task.configure { it.start.set marker }
    }

    @Override
    boolean isGenerating() {
        this.isGenerating.getOrElse false
    }

    @Override
    void publish(MavenPublication publication) {
        ChangelogUtils.setupChangelogGenerationForPublishing this.project, publication
    }

    @Override
    boolean isPublishAll() {
        this.publishingAll.getOrElse false
    }

    @Override
    void setPublishAll(boolean publishAll) {
        this.publishingAll.set publishAll
    }
}
