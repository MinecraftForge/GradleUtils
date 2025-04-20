/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import org.gradle.api.Plugin;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.PluginAware;
import org.gradle.api.problems.Problems;
import org.gradle.api.provider.ProviderFactory;

import javax.inject.Inject;

/** The entry point for the Gradle Utils plugin. Exists to create the {@linkplain GradleUtilsExtension extension}. */
abstract class GradleUtilsPlugin<T extends PluginAware & ExtensionAware> implements Plugin<T> {
    @Override
    public void apply(T target) {
        target.getExtensions().add(GradleUtilsExtension.class, GradleUtilsExtension.NAME, GradleUtilsExtensionImpl.create(
            target,
            this.getObjects(),
            this.getProviders()
        ));
    }

    @Inject
    public GradleUtilsPlugin() { }

    protected @Inject Problems getProblems() {
        return injectFailed();
    }

    protected @Inject ObjectFactory getObjects() {
        return injectFailed();
    }

    protected @Inject ProviderFactory getProviders() {
        return injectFailed();
    }

    private static <T> T injectFailed() {
        throw new IllegalStateException("Cannot use in current context");
    }
}
