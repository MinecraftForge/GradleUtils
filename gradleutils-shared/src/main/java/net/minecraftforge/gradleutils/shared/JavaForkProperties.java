/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.jspecify.annotations.Nullable;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Contains best-guesses at JVM properties that need to be passed into forked JVMs. This is mostly for network access
/// and proxies.
///
/// @see jdk.internal.util.SystemProps#initProperties()
/// @see <a
///  href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/doc-files/net-properties.html">JRE
/// Networking Properties</a>
@SuppressWarnings("JavadocReference")
abstract class JavaForkProperties implements ValueSource<Map<String, String>, ValueSourceParameters.None> {
    private static final List<String> CONTAINS = List.of(
        "socksProxyHost",
        "socksProxyPort",
        "socksProxyVersion",
        "socksNonProxyHosts"
    );

    private static final List<String> STARTS_WITH = List.of(
        "http.",
        "https.",
        "ftp.",
        "java.net.",
        "javax.net.ssl.",
        "jdk.tls."
    );

    private static boolean test(String key) {
        if (CONTAINS.contains(key))
            return true;

        for (var prefix : STARTS_WITH) {
            if (key.startsWith(prefix))
                return true;
        }

        return false;
    }

    @Inject
    public JavaForkProperties() { }

    @Override
    public @Nullable Map<String, String> obtain() {
        var systemProperties = System.getProperties();
        var forkProperties = new HashMap<String, String>(systemProperties.size());

        for (var property : systemProperties.entrySet()) {
            var key = property.getKey().toString();
            var value = property.getValue().toString();
            if (!test(key)) continue;

            forkProperties.put(key, value);
        }

        return forkProperties;
    }
}
