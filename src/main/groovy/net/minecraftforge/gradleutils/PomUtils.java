/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import net.minecraftforge.gradleutils.internal.PomUtilsInternalProxy;
import org.gradle.api.Action;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPomDeveloper;
import org.gradle.api.publish.maven.MavenPomLicense;

import java.util.Map;

/// Contains utilities to make working with [POMs][MavenPom] more ergonomic.
///
/// This can be accessed using the [gradleutils][GradleUtilsExtension] extension.
public interface PomUtils {
    /// Allows accessing [licenses][Licenses] from buildscripts using `gradleutils.pom.licenses`.
    ///
    /// @return A reference to the licenses
    /// @apiNote In GradleUtils 4.0.0, this method will instead return a [Map] of [Action]`<`[MavenPomLicense]`>`.
    /// @see Licenses
    Licenses getLicenses();

    /// Contains several licenses used by MinecraftForge to reduce needing to manually write them out in each project
    /// that uses one.
    ///
    /// @see #getLicenses()
    interface Licenses {
        //@formatter:off
        /// @see <a href="https://spdx.org/licenses/Apache-2.0.html">Apache License 2.0 on SPDX</a>
        Action<MavenPomLicense> Apache2_0 = PomUtilsInternalProxy.makeLicense("Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0");
        /// @see <a href="https://spdx.org/licenses/LGPL-2.1-only.html">GNU Lesser General Public License v2.1 only on SPDX</a>
        Action<MavenPomLicense> LGPLv2_1 = PomUtilsInternalProxy.makeLicense("LGPL-2.1-only", "https://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html");
        /// @see <a href="https://spdx.org/licenses/LGPL-3.0-only.html">GNU Lesser General Public License v3.0 only on SPDX</a>
        Action<MavenPomLicense> LGPLv3 = PomUtilsInternalProxy.makeLicense("LGPL-3.0-only", "https://www.gnu.org/licenses/lgpl-3.0-standalone.html");
        /// @see <a href="https://spdx.org/licenses/MIT.html">MIT License on SPDX</a>
        Action<MavenPomLicense> MIT = PomUtilsInternalProxy.makeLicense("MIT", "https://opensource.org/license/mit/");
        //@formatter:on
    }

    /// Contains several developers within the MinecraftForge organization to reduce needing to manually write them out
    /// in each project they contribute to.
    ///
    /// If a queried developer does not exist, it is automatically created with the input which is set to the
    /// {@linkplain MavenPomDeveloper#getId() ID} and {@linkplain MavenPomDeveloper#getName() name}.
    Map<String, Action<? super MavenPomDeveloper>> developers = PomUtilsInternalProxy.makeDevelopers();

    /// Adds details from the project's remote URL to the given POM.
    ///
    /// @param pom The POM to add details to
    /// @apiNote If the project does not have the `net.minecraftforge.gitversion` plugin applied, this method will fail.
    /// If you are not using Git Version, manually specify your project's URL using
    /// [#addRemoteDetails(MavenPom,String)].
    void addRemoteDetails(MavenPom pom);

    /**
     * Adds details from the given remote URL to the given POM.
     *
     * @param pom The pom to add details to
     * @param url The URL of the repository
     * @apiNote If you are using the {@code net.minecraftforge.gitversion} plugin, you can use
     * {@link #addRemoteDetails(MavenPom)} to use the URL discovered by Git Version instead of specifying it manually.
     */
    void addRemoteDetails(MavenPom pom, String url);
}
