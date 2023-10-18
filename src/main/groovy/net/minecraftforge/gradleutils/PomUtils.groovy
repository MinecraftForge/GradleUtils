/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPomDeveloper
import org.gradle.api.publish.maven.MavenPomIssueManagement
import org.gradle.api.publish.maven.MavenPomLicense
import org.gradle.api.publish.maven.MavenPomScm

/**
 * Utilities for making configuring a {@link MavenPom} more ergonomic.
 */
@CompileStatic
final class PomUtils {
    static final class Licenses {
        @Lazy
        public static final Closure Apache2_0 = { MavenPomLicense license ->
            license.name.set('Apache License, Version 2.0')
            license.url.set('https://www.apache.org/licenses/LICENSE-2.0.txt')
        }

        @Lazy
        public static final Closure LGPLv2_1 = { MavenPomLicense license ->
            license.name.set('LGPLv2.1')
            license.url.set('https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt')
        }

        @Lazy
        public static final Closure MIT = { MavenPomLicense license ->
            license.name.set('MIT')
            license.url.set('https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt')
        }
    }

    public static final Map<String, Action<? super MavenPomDeveloper>> Developers = [
            LexManos: makeDev('LexManos', 'Lex Manos'),
            Paint_Ninja: makeDev('Paint_Ninja'),
            SizableShrimp: makeDev('SizableShrimp'),
            cpw: makeDev('cpw'),
    ].withDefault(this.&makeDev) as Map<String, Action<? super MavenPomDeveloper>>

    private static Action<? super MavenPomDeveloper> makeDev(String id, String name = id) {
        return (MavenPomDeveloper developer) -> {
            developer.id.set(id)
            developer.name.set(name)
        } as Action<? super MavenPomDeveloper>
    }

    /**
     * Reduces boilerplate when setting up GitHub details in a {@link org.gradle.api.publish.maven.MavenPom}.
     * @param mavenPom The pom to configure
     * @param org The organisation or username the GitHub project is under
     * @param repo The name of the repository on GitHub
     */
    static void setGitHubDetails(MavenPom mavenPom, String org = 'MinecraftForge', String repo) {
        mavenPom.tap {
            scm { MavenPomScm scm ->
                scm.url.set("https://github.com/$org/$repo" as String)
                scm.connection.set("scm:git:git://github.com/$org/${repo}.git" as String)
                scm.developerConnection.set("scm:git:git@github.com:$org/${repo}.git" as String)
            }
            issueManagement { MavenPomIssueManagement issue ->
                issue.system.set('github')
                issue.url.set("https://github.com/$org/$repo/issues" as String)
            }
        }
    }
}
