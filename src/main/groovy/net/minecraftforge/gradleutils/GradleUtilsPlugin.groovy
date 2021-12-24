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

package net.minecraftforge.gradleutils

import net.minecraftforge.gradleutils.tasks.ExtractTeamCityProjectConfigurationTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class GradleUtilsPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        GradleUtilsExtension extension = project.extensions.create("gradleutils", GradleUtilsExtension.class, project)
        ChangelogGenerationExtension changelogGenerationExtension = project.extensions.create("changelog", ChangelogGenerationExtension.class, project)

        //Setup the teamcity project task.
        project.getTasks().register("setupTeamCityProject", ExtractTeamCityProjectConfigurationTask.class);
        GradleUtils.setupCITasks(project);
    }
}
