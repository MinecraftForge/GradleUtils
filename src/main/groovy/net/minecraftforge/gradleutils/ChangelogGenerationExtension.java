/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import org.gradle.api.Project;
import org.gradle.api.publish.maven.MavenPublication;

import javax.inject.Inject;

public class ChangelogGenerationExtension
{
    private final Project project;
    private boolean registerAllPublications = true;

    @Inject
    public ChangelogGenerationExtension(final Project project) {
        this.project = project;
        project.afterEvaluate(this::afterEvaluate);
    }

    public void fromMergeBase() {
        ChangelogUtils.setupChangelogGeneration(project);
    }

    public void fromTag(final String tag) {
        ChangelogUtils.setupChangelogGenerationFromTag(project, tag);
    }

    public void fromCommit(final String commit) {
        ChangelogUtils.setupChangelogGenerationFromCommit(project, commit);
    }

    public void setPublishAll(boolean value) {
        this.registerAllPublications = value;
    }

    public boolean isPublishAll() {
        return this.registerAllPublications;
    }

    public void disableAutomaticPublicationRegistration() {
        this.registerAllPublications = false;
    }

    public void publish(final MavenPublication mavenPublication) {
        ChangelogUtils.setupChangelogGenerationForPublishing(project, mavenPublication);
    }

    private void afterEvaluate(final Project project) {
        if (registerAllPublications)
            ChangelogUtils.setupChangelogGenerationOnAllPublishTasks(project);
    }
}
