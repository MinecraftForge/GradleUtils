/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import net.minecraftforge.gradleutils.gitversion.GitVersionExtension
import org.gradle.api.Action
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPomDeveloper
import org.gradle.api.publish.maven.MavenPomLicense
import org.jetbrains.annotations.ApiStatus

/**
 * Utilities for making configuring a {@code MavenPom} more ergonomic.
 *
 * @see <a href="https://docs.gradle.org/current/kotlin-dsl/gradle/org.gradle.api.publish.maven/-maven-pom/index.html?query=interface%20MavenPom">MavenPom</a>
 */
@CompileStatic
@SuppressWarnings('unused')
final class PomUtils {
    private final GitVersionExtension gitversion

    @PackageScope PomUtils(GitVersionExtension gitversion) {
        this.gitversion = gitversion
    }

    /** Allows accessing licenses from buildscripts using {@code gradleutils.pom.licenses}. */
    public static final Licenses licenses = new Licenses()
    @CompileStatic
    static final class Licenses {
        public static final Closure Apache2_0 = makeLicense('Apache-2.0', 'https://www.apache.org/licenses/LICENSE-2.0')
        public static final Closure LGPLv2_1 = makeLicense('LGPL-2.1-only', 'https://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html')
        public static final Closure LGPLv3 = makeLicense('LGPL-3.0-only', 'https://www.gnu.org/licenses/lgpl-3.0-standalone.html')
        public static final Closure MIT = makeLicense('MIT', 'https://opensource.org/license/mit/')

        private static Closure makeLicense(String name, String url) {
            return { MavenPomLicense license ->
                license.name.set name
                license.url.set url
                license.distribution.set 'repo'
            }
        }

        private Licenses() {}
    }

    // the IDE complains that the keys aren't actually Action<? super MavenPomDeveloper> if we make a map normally with Groovy
    // so, using Map.<>of() to make it explicit with the type arguments
    // HashMap so that withDefault() works
    //@formatter:off
    /** Common developers in the Minecraft Forge organization. */
    public static final Map<String, Action<? super MavenPomDeveloper>> developers = [
        LexManos     : makeDev('LexManos', 'Lex Manos'),
        Paint_Ninja  : makeDev('Paint_Ninja'),
        SizableShrimp: makeDev('SizableShrimp'),
        cpw          : makeDev('cpw'),
        Jonathing    : makeDev('Jonathing', 'me@jonathing.me', 'https://jonathing.me', 'America/New_York') // i'm overkill
    ].<String, Action<? super MavenPomDeveloper>>withDefault(this.&makeDev)
    //@formatter:on

    /**
     * @deprecated Casing changed.
     * @see #developers
     */
    @Deprecated(forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    public static final Map<String, Action<? super MavenPomDeveloper>> Developers = developers

    private static Action<? super MavenPomDeveloper> makeDev(String id, String name = id) {
        return { MavenPomDeveloper developer ->
            developer.id.set id
            developer.name.set name
        }
    }

    private static Action<? super MavenPomDeveloper> makeDev(String id, String name = id, String email, String url, String timezone) {
        return { MavenPomDeveloper developer ->
            developer.id.set id
            developer.name.set name
            developer.email.set email
            developer.url.set url
            developer.timezone.set timezone
        }
    }

    /**
     * Reduces boilerplate when setting up GitHub details in a {@link MavenPom}.
     *
     * @param pom The pom to configure
     */
    void setGitHubDetails(MavenPom pom) {
        setGitHubDetails(pom, this.gitversion)
    }

    /**
     * Reduces boilerplate when setting up GitHub details in a {@link MavenPom}. The organization is assumed to be
     * {@literal 'MinecraftForge'}.
     *
     * @param pom The pom to configure
     * @param repo The name of the repository on GitHub
     */
    static void setGitHubDetails(MavenPom pom, String repo) {
        this.setGitHubDetails(pom, 'MinecraftForge', repo)
    }

    /**
     * Reduces boilerplate when setting up GitHub details in a {@link MavenPom}.
     *
     * @param pom The pom to configure
     * @param organization The organization or username the GitHub project is under
     * @param repo The name of the repository on GitHub
     */
    static void setGitHubDetails(MavenPom pom, String organization, String repo) {
        setGitHubDetailsInternal pom, "github.com/${organization}/${repo}".toString()
    }

    static void setGitHubDetails(MavenPom pom, GitVersionExtension gitversion) {
        setGitHubDetailsInternal pom, stripProtocol(gitversion.url)
    }

    private static void setGitHubDetailsInternal(MavenPom pom, String url) {
        var fullURL = "https://${url}".toString()
        pom.url.set fullURL
        pom.scm { scm ->
            scm.url.set fullURL
            scm.connection.set "scm:git:git://${url}.git".toString()
            scm.developerConnection.set "scm:git:git@${url}.git".toString()
        }

        if (url.contains('github.com')) {
            pom.issueManagement { issues ->
                issues.system.set url.split('\\.', 2)[0]
                issues.url.set "https://${url}/issues".toString()
            }
            pom.ciManagement { ci ->
                ci.system.set 'github'
                ci.url.set "https://${url}/actions".toString()
            }
        }

        if (url.contains('MinecraftForge/')) {
            pom.organization { org ->
                org.name.set 'Forge Development LLC'
                org.url.set 'https://minecraftforge.net'
            }
        }
    }

    private static String stripProtocol(String url) {
        if (!url) return url

        final s = '://'
        int index = url.indexOf(s)
        return index == -1 ? url : url.substring(index + s.length())
    }
}
