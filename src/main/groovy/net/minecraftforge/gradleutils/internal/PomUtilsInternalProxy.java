package net.minecraftforge.gradleutils.internal;

import org.gradle.api.Action;
import org.gradle.api.publish.maven.MavenPomDeveloper;
import org.gradle.api.publish.maven.MavenPomLicense;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Map;

@VisibleForTesting
@ApiStatus.Internal
@Deprecated(forRemoval = true, since = "3.4.0")
@ApiStatus.ScheduledForRemoval(inVersion = "4.0.0")
public final class PomUtilsInternalProxy {
    public static Action<MavenPomLicense> makeLicense(String name, String url) {
        return PomUtilsInternal.makeLicense(name, url);
    }

    public static Map<String, Action<? super MavenPomDeveloper>> makeDevelopers() {
        return PomUtilsInternal.makeDevelopers();
    }
}
