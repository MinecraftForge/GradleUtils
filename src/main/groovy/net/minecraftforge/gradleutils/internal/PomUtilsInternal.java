/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.internal;

import net.minecraftforge.gradleutils.PomUtils;
import org.gradle.api.Action;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPomDeveloper;
import org.gradle.api.publish.maven.MavenPomLicense;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

interface PomUtilsInternal extends PomUtils, HasPublicType {
    @Override
    default TypeOf<?> getPublicType() {
        return TypeOf.typeOf(PomUtils.class);
    }

    static Action<MavenPomLicense> makeLicense(String name, String url) {
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
            "Jonathing", makeDev("Jonathing", "me@jonathing.me", "https://jonathing.me", "America/New_York")
        ));
    }

    private static Map<String, Action<? super MavenPomDeveloper>> makeDevelopers(Map<String, Action<? super MavenPomDeveloper>> defaults) {
        return new HashMap<>(defaults) {
            private static final @Serial long serialVersionUID = -9033218614762684158L;

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
            developer.getRoles().add("developer");
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
            developer.getRoles().add("developer");
        };
    }

    @Override
    @MustBeInvokedByOverriders
    default void addRemoteDetails(MavenPom pom, String url) {
        if (url == null || url.isBlank())
            throw new IllegalArgumentException();

        var strippedUrl = stripProtocol(url);
        var fullURL = "https://" + strippedUrl;
        pom.getUrl().set(fullURL);
        pom.scm(scm -> {
            scm.getUrl().set(fullURL);
            scm.getConnection().set("scm:git:git://%s.git".formatted(strippedUrl));
            scm.getDeveloperConnection().set("scm:git:git@%s.git".formatted(strippedUrl.replaceFirst("/", ":")));
        });

        // the rest is GitHub-exclusive information
        if (!strippedUrl.contains("github.com")) return;

        pom.issueManagement(issues -> {
            issues.getSystem().set("github");
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
