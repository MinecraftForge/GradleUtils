/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import net.minecraftforge.gradleutils.gitversion.GitVersionExtension
import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication

@CompileStatic
@SuppressWarnings('unused')
@PackageScope([PackageScopeTarget.CLASS, PackageScopeTarget.CONSTRUCTORS])
final class PomUtilsImpl implements PomUtils {
    private final Project project
    private final ProviderFactory providers

    PomUtilsImpl(Project project, ProviderFactory providers) {
        this.project = project
        this.providers = providers
    }

    @CompileStatic
    @PackageScope([PackageScopeTarget.CLASS, PackageScopeTarget.CONSTRUCTORS])
    static final class Licenses implements PomUtils.Licenses { }

    @Override
    void promote(MavenPublication publication, String promotionType) {
        PromoteArtifact.register this.project, publication, PromoteArtifact.Type.of(promotionType)
    }

    @Override
    void addRemoteDetails(MavenPom pom) {
        // check if we have git version
        final gitversion = this.project.extensions.findByType(GitVersionExtension)
        if (gitversion === null)
            throw new IllegalArgumentException()

        // try to get the url git version found
        final url = gitversion.url
        if (!url)
            throw new IllegalArgumentException()

        addRemoteDetails pom, url
    }
}
