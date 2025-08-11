/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import org.gradle.api.Action;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPomDeveloper;
import org.gradle.api.publish.maven.MavenPomLicense;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import java.util.HashMap;
import java.util.Map;

non-sealed interface PomUtilsInternal extends PomUtils, HasPublicType {
    @Override
    default TypeOf<?> getPublicType() {
        return TypeOf.typeOf(PomUtils.class);
    }

    non-sealed interface Licenses extends PomUtils.Licenses, HasPublicType {
        @Override
        default TypeOf<?> getPublicType() {
            return TypeOf.typeOf(PomUtils.Licenses.class);
        }
    }

    static Action<? extends MavenPomLicense> makeLicense(String name, String url) {
        return license -> {
            license.getName().set(name);
            license.getUrl().set(url);
            license.getDistribution().set("repo");
        };
    }

    static Map<String, Action<? super MavenPomDeveloper>> makeDevelopers() {
        return makeDevelopers(Map.of(
            "LexManos", makeDev("LexManos", "Lex Manos"),
            "Paint_Ninja", makeDev("Paint_Ninja"),
            "SizableShrimp", makeDev("SizableShrimp"),
            "cpw", makeDev("cpw"),
            "Jonathing", makeDev("Jonathing", "me@jonathing.me", "https://jonathing.me", "America/New_York")
        ));
    }

    private static Map<String, Action<? super MavenPomDeveloper>> makeDevelopers(Map<String, Action<? super MavenPomDeveloper>> defaults) {
        return new HashMap<>(defaults) {
            @Override
            public Action<? super MavenPomDeveloper> get(Object key) {
                this.ensure((String) key);
                return super.get(key);
            }

            private void ensure(String key) {
                if (!this.containsKey(key))
                    this.put(key, makeDev(key));
            }
        };
    }

    private static Action<? super MavenPomDeveloper> makeDev(String id) {
        return makeDev(id, id);
    }

    private static Action<? super MavenPomDeveloper> makeDev(String id, String name) {
        return developer -> {
            developer.getId().set(id);
            developer.getName().set(name);
        };
    }

    private static Action<? super MavenPomDeveloper> makeDev(String id, String email, String url, String timezone) {
        return makeDev(id, id, email, url, timezone);
    }

    private static Action<? super MavenPomDeveloper> makeDev(String id, String name, String email, String url, String timezone) {
        return developer -> {
            developer.getId().set(id);
            developer.getName().set(name);
            developer.getEmail().set(email);
            developer.getUrl().set(url);
            developer.getTimezone().set(timezone);
        };
    }

    @Override
    @MustBeInvokedByOverriders
    default void addRemoteDetails(MavenPom pom, String url) {
        if (url == null || url.isBlank())
            throw new IllegalArgumentException();

        var strippedUrl = stripProtocol(url);
        var fullURL = "https://" + url;
        pom.getUrl().set(fullURL);
        pom.scm(scm -> {
            scm.getUrl().set(fullURL);
            scm.getConnection().set("scm:git:git://%s.git".formatted(strippedUrl));
            scm.getDeveloperConnection().set("scm:git:git@%s.git".formatted(strippedUrl));
        });

        // the rest is GitHub-exclusive information
        if (!strippedUrl.contains("github.com")) return;

        pom.issueManagement(issues -> {
            issues.getSystem().set(url.split("\\.", 2)[0]);
            issues.getUrl().set(fullURL + "/issues");
        });
        pom.ciManagement(ci -> {
            ci.getSystem().set("github");
            ci.getUrl().set(fullURL + "/actions");
        });

        // the rest is Forge-exclusive information
        if (!strippedUrl.contains("github.com/MinecraftForge/")) return;

        pom.organization(organization -> {
            organization.getName().set(Constants.FORGE_ORG_NAME);
            organization.getUrl().set(Constants.FORGE_ORG_URL);
        });
    }

    private static String stripProtocol(String url) {
        int protocolIdx = url.indexOf("://");
        var ret = protocolIdx == -1 ? url : url.substring(protocolIdx + "://".length());
        return ret.endsWith("/") ? ret.substring(0, ret.length() - 1) : ret;
    }
}
