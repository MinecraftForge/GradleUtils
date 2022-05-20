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

import org.gradle.api.Action;
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
        project.afterEvaluate(project -> ChangelogUtils.setupChangelogGeneration(project, registerAllPublications));
    }

    public void fromTag(final String tag) {
        project.afterEvaluate(project -> ChangelogUtils.setupChangelogGenerationFromTag(project, tag, registerAllPublications));
    }

    public void fromCommit(final String commit) {
        project.afterEvaluate(project -> ChangelogUtils.setupChangelogGenerationFromCommit(project, commit, registerAllPublications));
    }

    public void disableAutomaticPublicationRegistration() {
        this.registerAllPublications = false;
    }

    public void publish(final MavenPublication mavenPublication) {
        ChangelogUtils.setupChangelogGenerationForPublishing(project, mavenPublication);
    }
}
