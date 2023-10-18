/*
 * GradleUtils
 * Copyright (C) 2021 Forge Development LLC
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
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
