/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import net.minecraftforge.gradleutils.shared.EnhancedProblems;
import org.gradle.api.problems.Severity;
import org.gradle.api.tasks.javadoc.Groovydoc;

import javax.inject.Inject;
import java.io.Serial;
import java.nio.charset.Charset;

abstract class GradleUtilsProblems extends EnhancedProblems {
    private static final @Serial long serialVersionUID = 3278085642147772954L;

    @Inject
    public GradleUtilsProblems() {
        super(GradleUtilsPlugin.NAME, GradleUtilsPlugin.DISPLAY_NAME);
    }

    //region GitHub Workflow Generation
    void ghWorkflowGitVersionMissing(String taskName) {
        this.report("gh-workflow-gitversion-missing", "GitHub Actions workflow is missing critical Git Version details", spec -> spec
            .details("""
                Task %s is generating a GitHub Actions workflow without critical data from Git Version.
                The workflow file will likely be incomplete or be missing details that may cause it to fail.""".formatted(taskName))
            .severity(Severity.WARNING)
            .stackLocation()
            .solution("Apply the Git Version Gradle plugin (net.minecraftforge.gitversion) to your project.")
            .solution("If the Git Version plugin is applied, double check the Git Version Gradle plugin implementation.")
            .solution("Manually add in the necessary details to the generated workflow file.")
            .solution(HELP_MESSAGE));
    }
    //endregion

    //region PomUtils
    void reportPomUtilsGitVersionMissing(Exception e) {
        report("pomutils-missing-url", "Cannot add POM remote details without URL", spec -> spec
            .details("""
                Cannot add POM remote details using `gradleutils.pom.addRemoteDetails` without the URL.
                If the Git Version plugin has not been applied, the URL must be manually specified as the second parameter.""")
            .severity(Severity.ERROR)
            .withException(e)
            .stackLocation()
            .solution("Apply the Git Version Gradle plugin (net.minecraftforge.gitversion) to your project.")
            .solution("Manually add the remote URL in `addRemoteDetails`.")
            .solution(HELP_MESSAGE));
    }
    //endregion

    //region JavaDoc Links
    void reportJavadocLinksNotOnClasspath(Throwable e) {
        this.report("javadoc-links-plugin-not-found", "JavaDoc Links plugin not in classpath", spec -> spec
            .details("""
                This project is using `resolveJavadocLinks` from FreeFair's JavaDoc Links plugin, but it was not loaded in the classpath!
                The javadoc links plugin must be loaded in the classpath before GradleUtils, even if it is not applied (i.e. in `settings.gradle`).
                
                This can be done by declaring it as so (in Groovy DSL):
                `id 'io.freefair.javadoc-links' version '8.14' apply false`""")
            .withException(e)
            .severity(Severity.ERROR)
            .stackLocation()
            .solution("Add the JavaDoc Links plugin before GradleUtils (use `apply(false)` if necessary).")
            .solution(HELP_MESSAGE));
    }
    //endregion

    //region
    void reportGroovydocIncorrectCharset(Groovydoc task) {
        this.report("groovydoc-incorrect-charset", "Groovydoc charset is incorrect", spec -> spec
            .details("""
                Groovydoc tasks cannot have their charsets manually set, and your default charset is not UTF-8.
                This may cause problems in the output of your Groovydoc.
                Affected task: %s
                Current charset: %s""".formatted(task.getName(), Charset.defaultCharset()))
            .severity(Severity.WARNING)
            .stackLocation()
            .solution("Set the JVM's default charset to UTF-8 using `propName.file.encoding=UTF-8` in your gradle.properties."));
    }
    //endregion
}
