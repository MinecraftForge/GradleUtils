/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.services

import io.freefair.gradle.plugins.maven.javadoc.JavadocLinkProvider
import io.freefair.gradle.plugins.maven.javadoc.JavadocLinkUtil
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.Nullable

class GradleUtilsJavadocLinkProvider implements JavadocLinkProvider {
    GradleUtilsJavadocLinkProvider() { }

    @Override
    @Nullable String getJavadocLink(String group, String artifact, String version) {
        // Allow Nokee and Remal's redistributions of Gradle API to link to Gradle's JavaDocs
        if ('gradle-api' == artifact && ('dev.gradleplugins' == group || 'name.remal.gradle-api' == group))
            JavadocLinkUtil.getGradleApiLink(GradleVersion.version(version))
    }
}
