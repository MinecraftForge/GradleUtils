/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import net.minecraftforge.gradleutils.gitversion.GitVersionExtension
import org.gradle.api.Action
import org.gradle.api.logging.Logger
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPomDeveloper
import org.gradle.api.publish.maven.MavenPomLicense

/** Utilities for making configuring a {@link MavenPom} more ergonomic. */
@CompileStatic
@SuppressWarnings('unused')
final class PomUtils {
    private final Logger logger
    private final ProviderFactory providers

    private final GitVersionExtension gitversion

    @PackageScope PomUtils(Logger logger, ProviderFactory providers, GitVersionExtension gitversion) {
        this.logger = logger
        this.providers = providers
        this.gitversion = gitversion
    }

    @CompileStatic
    static final class Licenses {
        public static final Closure Apache2_0 = { MavenPomLicense license ->
            license.name.set('Apache License, Version 2.0')
            license.url.set('https://www.apache.org/licenses/LICENSE-2.0.txt')
        }

        public static final Closure LGPLv2_1 = { MavenPomLicense license ->
            license.name.set('LGPLv2.1')
            license.url.set('https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt')
        }

        public static final Closure LGPLv3 = { MavenPomLicense license ->
            license.name.set('LGPLv3')
            license.url.set('https://www.gnu.org/licenses/lgpl-3.0.txt')
        }

        public static final Closure MIT = { MavenPomLicense license ->
            license.name.set('MIT')
            license.url.set('https://opensource.org/license/mit/')
        }

        private Licenses() {}
    }

    /** Allows accessing licenses from buildscripts using {@code gradleutils.pom.licenses}. */
    public static final Licenses licenses = new Licenses()

    /** Common developers in the Minecraft Forge organization. */
    public static final Map<String, Action<? super MavenPomDeveloper>> Developers = [
        LexManos     : makeDev('LexManos', 'Lex Manos'),
        Paint_Ninja  : makeDev('Paint_Ninja'),
        SizableShrimp: makeDev('SizableShrimp'),
        cpw          : makeDev('cpw'),
        Jonathing    : makeDev('Jonathing', 'me@jonathing.me', 'https://jonathing.me', 'America/New_York') // i'm overkill
    ].withDefault(this.&makeDev) as Map<String, Action<? super MavenPomDeveloper>>

    private static Action<? super MavenPomDeveloper> makeDev(String id, String name = id) {
        return { MavenPomDeveloper developer ->
            developer.id.set(id)
            developer.name.set(name)
        }
    }

    private static Action<? super MavenPomDeveloper> makeDev(String id, String name = id, String email, String url, String timezone) {
        return { MavenPomDeveloper developer ->
            developer.id.set(id)
            developer.name.set(name)
            developer.email.set(email)
            developer.url.set(url)
            developer.timezone.set(timezone)
        }
    }

    /**
     * Reduces boilerplate when setting up GitHub details in a {@link MavenPom}.
     *
     * @param pom The pom to configure
     */
    void setGitHubDetails(MavenPom pom) {
        this.setGitHubDetails(pom, '')
    }

    /**
     * Reduces boilerplate when setting up GitHub details in a {@link MavenPom}. The organization is assumed to be
     * {@literal 'MinecraftForge'}.
     *
     * @param pom The pom to configure
     * @param repo The name of the repository on GitHub
     */
    void setGitHubDetails(MavenPom pom, String repo) {
        this.setGitHubDetails(pom, 'MinecraftForge', repo)
    }

    /**
     * Reduces boilerplate when setting up GitHub details in a {@link MavenPom}.
     *
     * @param pom The pom to configure
     * @param organization The organization or username the GitHub project is under
     * @param repo The name of the repository on GitHub
     */
    void setGitHubDetails(MavenPom pom, String organization, String repo) {
        // always add this
        pom.organization { org ->
            org.name.set 'Forge Development LLC'
            org.url.set 'https://minecraftforge.net'
        }

        var inCI = GradleUtils.hasEnvVar('GITHUB_ACTIONS', this.providers).get().booleanValue()

        var remoteUrl = stripProtocol(this.gitversion.version.info.url)
        var url = remoteUrl
        if (organization && repo) {
            url = "github.com/${organization}/${repo}".toString()

            if (url && url == remoteUrl) {
                this.logger.warn """
                    WARNING: The repository name was specified in the 'setGitHubDetails' method, but it was already present in the Git remote URL.
                    This is redundant and may cause issues if the remote repository URL changes in the future."""
            }
        }

        if (!url) {
            this.logger.warn """
                WARNING: The GitHub URL for this repo could not be automatically determined by GitVersion.
                This is likely due to the repository not having any remotes, or not having one set."""
            if (inCI)
                throw new IllegalStateException('GitHub URL could not be determined, which is required in CI')

            return
        }

        if (!url.contains('github.com')) {
            this.logger.warn """
                WARNING: The repository URL found or created in 'setGitHubDetails' does not include 'github.com'
                This is problematic since all Minecraft Forge projects are hosted on GitHub.
                Found url: ${url}"""
        }

        if (!url.contains('github.com/MinecraftForge')) {
            this.logger.warn """
                WARNING: The repository URL found or created in 'setGitHubDetails' does not include 'github.com/MinecraftForge'
                This is problematic if you are attempting to publish this project, especially from GitHub Actions.
                Found url: ${url}"""
        }

        var fullURL = "https://${url}".toString()
        pom.url.set fullURL
        pom.scm { scm ->
            scm.url.set fullURL
            scm.connection.set "scm:git:git://${url}.git".toString()
            scm.developerConnection.set "scm:git:git@${url}.git".toString()
        }
        pom.issueManagement { issues ->
            issues.system.set url.split('\\.', 2)[0]
            issues.url.set "https://${url}/issues".toString()
        }
        pom.ciManagement { ci ->
            ci.system.set 'github'
            ci.url.set "https://${url}/actions".toString()
        }
    }

    private static String stripProtocol(String url) {
        if (!url) return url

        final s = '://'
        int index = url.indexOf(s)
        return index == -1 ? url : url.substring(index + s.length())
    }
}
