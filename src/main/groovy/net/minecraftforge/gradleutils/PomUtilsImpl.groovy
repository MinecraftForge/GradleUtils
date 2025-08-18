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

    final Licenses licenses = this.objects.newInstance(Licenses)

    @CompileStatic
    @PackageScope static abstract class Licenses implements PomUtilsInternal.Licenses {
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
            // - Project contains an extension named 'gitversion'
            // - The extension contains method '#getUrl()' or property 'url'
            this.target.extensions.getByName('gitversion').url

            // IN CASE you need to migrate to Java, here is the Java equivalent of this, safe to compile:
            //org.codehaus.groovy.runtime.InvokerHelper.getProperty(this.project.extensions.getByName("gitversion"), "url")
        } catch (Exception e) {
            throw this.problems.pomUtilsGitVersionMissing(e)
        }
    }
}
