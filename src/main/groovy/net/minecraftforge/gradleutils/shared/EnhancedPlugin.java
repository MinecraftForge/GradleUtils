package net.minecraftforge.gradleutils.shared;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.jetbrains.annotations.ApiStatus;

import javax.inject.Inject;
import java.io.File;
import java.util.Objects;

/// The enhanced plugin contains several helper members to assist in making Gradle plugins as clean as possible without
/// needing to duplicate code across projects.
///
/// @param <T> The type of target
@ApiStatus.OverrideOnly
public abstract class EnhancedPlugin<T> implements Plugin<T> {
    private final String name;
    private final String displayName;

    private T target;
    private final EnhancedProblems problemsInternal;

    /**
     * @see <a href="https://docs.gradle.org/current/userguide/service_injection.html#objectfactory">ObjectFactory
     * Service Injection</a>
     */
    protected abstract @Inject ObjectFactory getObjects();

    /**
     * @see <a href="https://docs.gradle.org/current/userguide/service_injection.html#providerfactory">ProviderFactory
     * Service Injection</a>
     */
    protected abstract @Inject ProviderFactory getProviders();

    /// This constructor must be called by all subclasses using a public constructor annotated with [Inject]. The name
    /// and display name passed in are used in a minimal instance of [EnhancedProblems], which is used to set up the
    /// plugin's [global][#getGlobalCaches()] and [local][#getLocalCaches()] caches. Additionally, the name is used to
    /// create the cache folders (`minecraftforge/name`).
    ///
    /// @param name        The name for this plugin (must be machine-friendly)
    /// @param displayName The display name for this plugin
    protected EnhancedPlugin(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;

        this.problemsInternal = this.getObjects().newInstance(EnhancedProblems.Minimal.class, name, displayName);
    }

    /// This method is used by Gradle to apply this plugin. You should instead override [#setup(T)] to do plugin setup.
    ///
    /// @param target The target for this plugin
    @Override
    public final void apply(T target) {
        this.setup(this.target = target);
    }

    /// Called when this plugin is applied to do setup work.
    ///
    /// @param target The target for this plugin (can also get after setup with [#getTarget()])
    public abstract void setup(T target);

    /// Gets the target for this plugin. This will throw an exception if this is called before application (i.e. through
    /// early usage of [#getGlobalCaches()]).
    ///
    /// @return The plugin target
    /// @throws RuntimeException If this plugin is not yet applied
    protected final T getTarget() {
        try {
            return Objects.requireNonNull(this.target);
        } catch (Exception e) {
            throw this.problemsInternal.pluginNotYetApplied(e);
        }
    }

    final EnhancedProblems getProblemsInternal() {
        return this.problemsInternal;
    }


    /* TOOLS */

    /// Gets a provider to the file for a [Tool] to be used. The tool's state is managed by Gradle through the
    /// [org.gradle.api.provider.ValueSource] API and will not cause caching issues.
    ///
    /// @param tool The tool to get
    /// @return A provider for the tool file
    @SuppressWarnings("deprecation") // deprecation intentional, please use this method
    public Provider<File> getTool(Tool tool) {
        return tool.get(this.getGlobalCaches(), this.getProviders());
    }


    /* CACHES */

    private final Lazy<DirectoryProperty> globalCaches = Lazy.simple(this::makeGlobalCaches);

    /// Gets the global caches to be used for this plugin. These caches persist between projects and should be used to
    /// eliminate excess work done by projects that request the same data.
    ///
    /// It is stored in `~/.gradle/caches/minecraftforge/plugin`.
    ///
    /// @return The global caches
    /// @throws RuntimeException If this plugin cannot access global caches (i.e. the target is not [Project] or
    ///                          [org.gradle.api.initialization.Settings])
    public final DirectoryProperty getGlobalCaches() {
        return this.globalCaches.get();
    }

    private DirectoryProperty makeGlobalCaches() {
        try {
            var startParameter = ((Gradle) InvokerHelper.getPropertySafe(this.target, "gradle")).getStartParameter();
            var gradleUserHomeDir = this.getObjects().directoryProperty().fileValue(startParameter.getGradleUserHomeDir());

            return this.getObjects().directoryProperty().convention(
                gradleUserHomeDir.dir("caches/minecraftforge/" + this.name).map(this.problemsInternal.ensureFileLocation())
            );
        } catch (Exception e) {
            throw this.problemsInternal.illegalPluginTarget(
                new IllegalArgumentException("Failed to get %s global caches directory for target: %s".formatted(this.displayName, this.target), e),
                "types with access to Gradle (#getGradle()Lorg/gradle/api/invocation/Gradle), such as projects or settings."
            );
        }
    }

    private final Lazy<DirectoryProperty> localCaches = Lazy.simple(this::makeLocalCaches);

    /// Gets the local caches to be used for this plugin. Data done by tasks that should not be shared between projects
    /// should be stored here.
    ///
    /// It is located in `project/build/minecraftforge/plugin`.
    ///
    /// @return The global caches
    /// @throws RuntimeException If this plugin cannot access global caches (i.e. the target is not [Project] or
    ///                          [org.gradle.api.initialization.Settings])
    public final DirectoryProperty getLocalCaches() {
        return this.localCaches.get();
    }

    private DirectoryProperty makeLocalCaches() {
        try {
            DirectoryProperty workingProjectBuildDir;
            if (this.target instanceof Project project) {
                workingProjectBuildDir = project.getLayout().getBuildDirectory();
            } else {
                var startParameter = ((Gradle) InvokerHelper.getPropertySafe(this.target, "gradle")).getStartParameter();
                workingProjectBuildDir = this.getObjects().directoryProperty().fileValue(new File(Objects.requireNonNullElseGet(startParameter.getProjectDir(), startParameter::getCurrentDir), "build"));
            }

            return this.getObjects().directoryProperty().convention(
                workingProjectBuildDir.dir("minecraftforge/" + this.name).map(this.problemsInternal.ensureFileLocation())
            );
        } catch (Exception e) {
            throw this.problemsInternal.illegalPluginTarget(
                new IllegalArgumentException("Failed to get %s local caches directory for target: %s".formatted(this.displayName, this.getTarget()), e),
                "projects or types with access to Gradle (#getGradle()Lorg/gradle/api/invocation/Gradle), such as settings."
            );
        }
    }
}
