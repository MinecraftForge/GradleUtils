/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.services;

import io.freefair.gradle.plugins.maven.javadoc.JavadocLinkProvider;
import io.freefair.gradle.plugins.maven.javadoc.JavadocLinkUtil;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.Nullable;

/// Service for custom JavaDoc links not provided by the plugin
public class GradleUtilsJavadocLinkProvider implements JavadocLinkProvider {
    /// Invoked by the [java.util.ServiceLoader].
    public GradleUtilsJavadocLinkProvider() { }

    @Override
    public @Nullable String getJavadocLink(String group, String artifact, String version) {
        // Allow Nokee and Remal's redistributions of Gradle API to link to Gradle's JavaDocs
        if ("gradle-api".equals(artifact) && ("dev.gradleplugins".equals(group) || "name.remal.gradle-api".equals(group)))
            return JavadocLinkUtil.getGradleApiLink(GradleVersion.version(version));

        return null;
    }
}
