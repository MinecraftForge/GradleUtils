/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.internal

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.publish.maven.MavenPom

import javax.inject.Inject

@CompileStatic
@PackageScope abstract class PomUtilsImpl implements PomUtilsInternal {
    private final ExtensionAware target
    private final GradleUtilsProblems problems

    protected abstract @Inject ObjectFactory getObjects()

    @Inject
    PomUtilsImpl(ExtensionAware target) {
        this.target = target
        this.problems = this.objects.newInstance(GradleUtilsProblems)
    }

    final Licenses licenses = this.objects.newInstance(LicensesImpl)

    @CompileStatic
    @PackageScope static abstract class LicensesImpl implements Licenses {
        @Inject
        Licenses() { }
    }

    @Override
    @CompileDynamic
    void addRemoteDetails(MavenPom pom) {
        try {
            // NOTE: There isn't a very good way of doing this.
            // The point of this method is to use Git Version to get the project's URL without needing to manually set it.
            // The Git Version Gradle plugin now exists within its own project though, outside of GradleUtils.
            // If GradleUtils were to depend on Git Version, even as compile-only, it might lead to cyclic dependency issues.
            // Editing the PomUtilsImpl meta-class from the Git Version plugin itself is too risky.
            // As such, this contract must ALWAYS be true:
            // - Project or Gradle contains an extension named 'gitversion'
            //   - Applying Git Version to Settings will add the extension to settings and gradle
            // - The extension contains method '#getUrl()' or property 'url'
            def url
            try {
                url = this.target.extensions.getByName('gitversion').url
            } catch (Exception e) {
                try {
                    url = this.target.gradle.extensions.getByName('gitversion').url
                } catch (Exception suppressed) {
                    throw e.tap { addSuppressed(suppressed) }
                }
            }

            this.addRemoteDetails(pom, url)
        } catch (Exception e) {
            this.problems.reportPomUtilsGitVersionMissing(e)
        }
    }
}
