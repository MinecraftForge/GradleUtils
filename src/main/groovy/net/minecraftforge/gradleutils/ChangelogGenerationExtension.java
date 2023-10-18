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
    public ChangelogGenerationExtension(final Project project) {this.project = project;}

    public void fromMergeBase() {
        ChangelogUtils.setupChangelogGeneration(project);
        project.afterEvaluate(this::afterEvaluate);
    }

    public void fromTag(final String tag) {
        ChangelogUtils.setupChangelogGenerationFromTag(project, tag);
        project.afterEvaluate(this::afterEvaluate);
    }

    public void fromCommit(final String commit) {
        ChangelogUtils.setupChangelogGenerationFromCommit(project, commit);
        project.afterEvaluate(this::afterEvaluate);
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
