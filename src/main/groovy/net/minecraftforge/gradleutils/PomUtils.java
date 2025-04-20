package net.minecraftforge.gradleutils;

import org.gradle.api.Action;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPomDeveloper;
import org.gradle.api.publish.maven.MavenPomLicense;
import org.gradle.api.publish.maven.MavenPublication;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains utilities to make working with {@link MavenPom POMs} more ergonomic.
 * <p>This can be accessed by {@linkplain org.gradle.api.Project projects} using the
 * {@link GradleUtilsExtension.ForProject gradleutils} extension.</p>
 */
public sealed interface PomUtils permits PomUtilsImpl {
    /**
     * Allows accessing licenses from buildscripts using {@code gradleutils.pom.licenses}.
     *
     * @see Licenses
     */
    Licenses licenses = new PomUtilsImpl.Licenses();

    /**
     * Contains several licenses used by MinecraftForge to reduce needing to manually write them out in each project
     * that uses one.
     *
     * @see #licenses
     */
    sealed interface Licenses permits PomUtilsImpl.Licenses {
        Action<? extends MavenPomLicense>
            Apache2_0 = makeLicense("Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
            LGPLv2_1 = makeLicense("LGPL-2.1-only", "https://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html"),
            LGPLv3 = makeLicense("LGPL-3.0-only", "https://www.gnu.org/licenses/lgpl-3.0-standalone.html"),
            MIT = makeLicense("MIT", "https://opensource.org/license/mit/");
    }

    /**
     * Contains several developers within the MinecraftForge organization to reduce needing to manually write them out
     * in each project they contribute to.
     * <p>If a queried developer does not exist, it is automatically created with the input which is set to the
     * {@linkplain MavenPomDeveloper#getId() ID} and {@linkplain MavenPomDeveloper#getName() name}.</p>
     */
    Map<String, Action<? super MavenPomDeveloper>> developers = makeDevelopers(Map.of(
        "LexManos", makeDev("LexManos", "Lex Manos"),
        "Paint_Ninja", makeDev("Paint_Ninja"),
        "SizableShrimp", makeDev("SizableShrimp"),
        "cpw", makeDev("cpw"),
        "Jonathing", makeDev("Jonathing", "me@jonathing.me", "https://jonathing.me", "America/New_York")
    ));

    /**
     * Promotes the given publication's artifact as the latest to the
     * <a href="https://files.minecraftforge.net/project_index.html">Forge Files site's project index.</a>
     *
     * @param publication The publication to promote
     * @apiNote Should only be used by MinecraftForge projects.
     */
    @ApiStatus.Internal
    default void promote(MavenPublication publication) {
        this.promote(publication, "latest");
    }

    /**
     * Promotes the given publication's artifact, using the given promotion type, to the
     * <a href="https://files.minecraftforge.net/project_index.html">Forge Files site's project index.</a>
     *
     * @param publication The publication to promote
     * @apiNote Should only be used by MinecraftForge projects.
     */
    @ApiStatus.Internal
    void promote(MavenPublication publication, String promotionType);

    /**
     * Adds MinecraftForge-specific details to the given POM.
     *
     * @param pom The POM to add details to
     */
    static void addForgeDetails(MavenPom pom) {
        pom.organization(organization -> {
            organization.getName().set("Forge Development LLC");
            organization.getUrl().set("https://minecraftforge.net");
        });
    }

    /**
     * Adds details from the project's remote URL to the given POM.
     *
     * @param pom The POM to add details to
     * @apiNote If the project does not have the
     * {@link net.minecraftforge.gradleutils.gitversion.GitVersionExtension net.minecraftforge.gitversion} plugin
     * applied, this method will fail. If you are not using Git Version, manually specify your project's URL using
     * {@link #addRemoteDetails(MavenPom, String)}.
     */
    void addRemoteDetails(MavenPom pom);

    /**
     * Adds details from the given remote URL to the given POM.
     *
     * @param pom The pom to add details to
     * @param url The URL of the repository
     * @apiNote If you are using the
     * {@link net.minecraftforge.gradleutils.gitversion.GitVersionExtension net.minecraftforge.gitversion} plugin, you
     * can use {@link #addRemoteDetails(MavenPom)} to use the URL discovered by Git Version instead of specifying it
     * manually.
     */
    static void addRemoteDetails(MavenPom pom, String url) {
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
    }


    /* IMPLEMENTATIONS */

    private static Action<? extends MavenPomLicense> makeLicense(String name, String url) {
        return license -> {
            license.getName().set(name);
            license.getUrl().set(url);
            license.getDistribution().set("repo");
        };
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

    private static String stripProtocol(String url) {
        int protocolIdx = url.indexOf("://");
        var ret = protocolIdx == -1 ? url : url.substring(protocolIdx + "://".length());
        return ret.endsWith("/") ? ret.substring(0, ret.length() - 1) : ret;
    }
}
