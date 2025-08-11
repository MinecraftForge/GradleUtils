/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.services;

import io.freefair.gradle.plugins.maven.javadoc.JavadocLinkProvider;
import io.freefair.gradle.plugins.maven.javadoc.JavadocLinkUtil;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.Nullable;

/// Service to allow Nokee and Remal's redistributions of the Gradle API to link to Gradle's JavaDocs website.
public class GradleAPIJavadocLinkProvider implements JavadocLinkProvider {
    /// Invoked by the [java.util.ServiceLoader].
    public GradleAPIJavadocLinkProvider() { }

    @Override
    public @Nullable String getJavadocLink(String group, String artifact, String version) {
        if (!"gradle-api".equals(artifact)) return null;
        if (!"dev.gradleplugins".equals(group) && !"name.remal.gradle-api".equals(group)) return null;

        return JavadocLinkUtil.getGradleApiLink(GradleVersion.version(version));
    }
}
