/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.gitversion;

import groovy.transform.CompileStatic;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.configuration.BuildFeatures;
import org.gradle.api.file.BuildLayout;
import org.gradle.api.file.Directory;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.PluginAware;
import org.gradle.api.problems.Problems;
import org.gradle.api.provider.ProviderFactory;

import javax.inject.Inject;

abstract class GitVersionPlugin<T extends PluginAware & ExtensionAware> implements Plugin<T> {
    @Inject
    public GitVersionPlugin() { }

    @Override
    public void apply(T target) {
        Directory projectDirectory;
        if (target instanceof Project)
            projectDirectory = this.getProjectLayout().getProjectDirectory();
        else if (target instanceof Gradle)
            projectDirectory = this.getBuildLayout().getRootDirectory();
        else
            throw new IllegalStateException("Cannot determine project directory");

        target.getExtensions().add(GitVersionExtension.class, GitVersionExtension.NAME, new GitVersionExtensionImpl(
            projectDirectory,
            this.getObjects(),
            this.getProviders(),
            this.getBuildFeatures()
        ));
    }

    protected @Inject Problems getProblems() {
        return injectFailed();
    }

    protected @Inject ObjectFactory getObjects() {
        return injectFailed();
    }

    protected @Inject ProjectLayout getProjectLayout() {
        return injectFailed();
    }

    protected @Inject BuildLayout getBuildLayout() {
        return injectFailed();
    }

    protected @Inject ProviderFactory getProviders() {
        return injectFailed();
    }

    protected @Inject BuildFeatures getBuildFeatures() {
        return injectFailed();
    }

    private static <T> T injectFailed() {
        throw new IllegalStateException("Cannot use in current context");
    }
}
