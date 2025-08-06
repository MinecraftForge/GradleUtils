/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import net.minecraftforge.gradleutils.shared.SharedUtil
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.publish.maven.MavenPom

import javax.inject.Inject

@CompileStatic
@PackageScope abstract class PomUtilsImpl implements PomUtilsInternal {
    private final Project project
    private final GradleUtilsProblems problems

    protected abstract @Inject ObjectFactory getObjects()

    @Inject
    PomUtilsImpl(Project project) {
        this.project = project
        this.problems = this.objects.newInstance(GradleUtilsProblems)
    }

    final Licenses licenses = this.objects.newInstance(Licenses)

    @CompileStatic
    @PackageScope static abstract class Licenses implements PomUtilsInternal.Licenses {
        @Inject
        Licenses() { }
    }

    @Override
    @CompileDynamic
    void addRemoteDetails(MavenPom pom) {
        // Overridden by Git Version Plugin
        throw this.problems.pomUtilsGitVersionMissing()
    }

    @Override
    void addRemoteDetails(MavenPom pom, String url) {
        super.addRemoteDetails(pom, url)

        if (url.contains('github.com/MinecraftForge/')) {
            SharedUtil.ensureAfterEvaluate(this.project) {
                pom.organization { organization ->
                    if (organization.name.orNull != Constants.FORGE_ORG_NAME)
                        this.problems.reportPomUtilsForgeProjWithoutForgeOrg()
                }
            }
        }
    }
}
