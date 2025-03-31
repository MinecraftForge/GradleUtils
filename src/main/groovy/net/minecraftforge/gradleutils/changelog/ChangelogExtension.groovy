/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.changelog

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nullable

import javax.inject.Inject

/** The heart of the Changelog plugin. This extension is used to enable and partially configure the changelog generation task. */
@CompileStatic
class ChangelogExtension {
    public static final String NAME = 'changelog'

    @PackageScope final Project project

    /** @deprecated The Git root is automatically discovered by Git Version on Changelog generation. */
    @Deprecated(forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    @Nullable Directory gitRoot

    boolean publishAll = true
    @PackageScope boolean isGenerating
    private @Lazy TaskProvider<GenerateChangelog> task = {
        this.isGenerating = true

        ChangelogUtils.setupChangelogTask(this.project) { task ->
            this.project.afterEvaluate { project ->
                if (this.gitRoot) {
                    task.configure {
                        it.gitDirectory.set gitRoot
                    }
                }

                if (this.publishAll)
                    ChangelogUtils.setupChangelogGenerationOnAllPublishTasks project
            }
        }
    }()

    /** @deprecated This constructor will be made package-private in GradleUtils 3.0 */
    @Inject
    @Deprecated(forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    ChangelogExtension(Project project) {
        this.project = project
    }

    void fromBase() {
        from null
    }

    void from(String marker) {
        this.task.configure { it.start.set marker }
    }

    private static boolean fromTagDeprecationWarning
    @Deprecated(forRemoval = true, since = '2.4')
    void fromTag(String tag) {
        if (!fromTagDeprecationWarning) {
            this.project.logger.warn "WARNING: This project is still using 'changelog.fromTag'. It has been deprecated and will be removed in GradleUtils 3.0. Consider using 'changelog.from' instead."
            fromTagDeprecationWarning = true
        }

        this.from tag
    }

    private static boolean fromCommitDeprecationWarning
    @Deprecated(forRemoval = true, since = '2.4')
    void fromCommit(String commit) {
        if (!fromCommitDeprecationWarning) {
            this.project.logger.warn "WARNING: This project is still using 'changelog.fromCommit'. It has been deprecated and will be removed in GradleUtils 3.0. Consider using 'changelog.from' instead."
            fromCommitDeprecationWarning = true
        }

        this.from commit
    }

    private static boolean disableAutomaticPublicationRegistrationDeprecationWarning
    @Deprecated(forRemoval = true, since = '2.4')
    void disableAutomaticPublicationRegistration() {
        if (!disableAutomaticPublicationRegistrationDeprecationWarning) {
            this.project.logger.warn "WARNING: This project is still using 'changelog.disableAutomaticPublicationRegistration'. It has been deprecated and will be removed in GradleUtils 3.0. Consider using 'changelog.publishAll = false' instead."
            disableAutomaticPublicationRegistrationDeprecationWarning = true
        }

        this.publishAll = false
    }

    void publish(MavenPublication publication) {
        ChangelogUtils.setupChangelogGenerationForPublishing this.project, publication
    }
}
