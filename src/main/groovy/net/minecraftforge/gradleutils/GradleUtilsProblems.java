/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import net.minecraftforge.gradleutils.shared.EnhancedProblems;
import org.gradle.api.problems.Severity;

import javax.inject.Inject;

abstract class GradleUtilsProblems extends EnhancedProblems {
    @Inject
    public GradleUtilsProblems() {
        super(GradleUtilsPlugin.NAME, GradleUtilsPlugin.DISPLAY_NAME);
    }

    //region GitHub Workflow Generation
    void ghWorkflowGitVersionMissing(String taskName) {
        this.getReporter().report(id("gh-workflow-gitversion-missing", "GitHub Actions workflow is missing critical Git Version details"), spec -> spec
            .details("""
                Task %s is generating a GitHub Actions workflow without critical data from Git Version.
                The workflow file will likely be incomplete or be missing details that may cause it to fail.""".formatted(taskName))
            .severity(Severity.WARNING)
            .stackLocation()
            .solution("Apply the Git Version Plugin (net.minecraftforge.gitversion) to your project.")
            .solution("If the Git Version plugin is applied, double check the Git Version Gradle plugin implementation.")
            .solution("Manually add in the necessary details to the generated workflow file.")
            .solution(HELP_MESSAGE));
    }
    //endregion

    //region PomUtils
    RuntimeException pomUtilsGitVersionMissing() {
        return this.getReporter().throwing(new UnsupportedOperationException(), id("pomutils-missing-url", "Cannot add POM remote details without URL"), spec -> spec
            .details("""
                Cannot add POM remote details using `gradleutils.pom.addRemoteDetails` without the URL.
                If the Git Version plugin has not been applied, the URL must be manually specified as the second parameter.""")
            .severity(Severity.ERROR)
            .stackLocation()
            .solution("Apply the Git Version Plugin (net.minecraftforge.gitversion) to your project.")
            .solution("Manually add the remote URL in `addRemoteDetails`.")
            .solution(HELP_MESSAGE));
    }

    void reportPomUtilsForgeProjWithoutForgeOrg() {
        this.getReporter().report(id("pomutils-forge-proj-missing-forge-org", "Detected Forge project is missing Forge organization details"), spec -> spec
            .details("""
                This project was autodetected as a MinecraftForge project, but `gradleutils.pom.addForgeDetails` was not used.""")
            .severity(Severity.ADVICE)
            .stackLocation()
            .solution("Consider using `gradleutils.pom.addForgeDetails`"));
    }
    //endregion
}
