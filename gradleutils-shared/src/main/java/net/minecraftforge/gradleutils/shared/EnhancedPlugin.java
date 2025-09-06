/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.BuildLayout;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.initialization.Settings;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import javax.inject.Inject;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.Callable;

/// The enhanced plugin contains several helper members to assist in making Gradle plugins as clean as possible without
/// needing to duplicate code across projects.
///
/// @param <T> The type of target
public abstract class EnhancedPlugin<T> implements Plugin<T>, EnhancedPluginAdditions {
    private final String name;
    private final String displayName;
    private final @Nullable String toolsExtName;

    private @UnknownNullability T target;
    private ToolsExtensionImpl tools = this.getObjects().newInstance(ToolsExtensionImpl.class, (Callable<? extends JavaToolchainService>) this::toolchainsForTools);
    private final EnhancedProblems problemsInternal;

    /// The object factory provided by Gradle services.
    ///
    /// @return The object factory
    /// @see <a href="https://docs.gradle.org/current/userguide/service_injection.html#objectfactory">ObjectFactory
    /// Service Injection</a>
    protected abstract @Inject ObjectFactory getObjects();

    /// The project layout provided by Gradle services.
    ///
    /// @return The build layout
    /// @see <a href="https://docs.gradle.org/current/userguide/service_injection.html#buildlayout">BuildLayout
    /// Service Injection</a>
    protected abstract @Inject ProjectLayout getProjectLayout();

    /// The build layout provided by Gradle services.
    ///
    /// @return The build layout
    /// @see <a href="https://docs.gradle.org/current/userguide/service_injection.html#buildlayout">BuildLayout
    /// Service Injection</a>
    protected abstract @Inject BuildLayout getBuildLayout();

    /// The provider factory provided by Gradle services.
    ///
    /// @return The provider factory
    /// @see <a href="https://docs.gradle.org/current/userguide/service_injection.html#providerfactory">ProviderFactory
    /// Service Injection</a>
    protected abstract @Inject ProviderFactory getProviders();

    /// The Java toolchain service provided by Gradle services.
    ///
    /// @return The Java toolchain service
    protected abstract @Inject JavaToolchainService getJavaToolchains();

    /// This constructor must be called by all subclasses using a public constructor annotated with [Inject]. The name
    /// and display name passed in are used in a minimal instance of [EnhancedProblems], which is used to set up the
    /// plugin's [global][#globalCaches()] and [local][#localCaches()] caches. Additionally, the name is used to
    /// create the cache folders (`minecraftforge/name`).
    ///
    /// @param name        The name for this plugin (must be machine-friendly)
    /// @param displayName The display name for this plugin
    protected EnhancedPlugin(String name, String displayName) {
        this(name, displayName, null);
    }

    /// This constructor must be called by all subclasses using a public constructor annotated with [Inject]. The name
    /// and display name passed in are used in a minimal instance of [EnhancedProblems], which is used to set up the
    /// plugin's [global][#globalCaches()] and [local][#localCaches()] caches. Additionally, the name is used to
    /// create the cache folders (`minecraftforge/name`).
    ///
    /// @param name        The name for this plugin (must be machine-friendly)
    /// @param displayName The display name for this plugin
    protected EnhancedPlugin(String name, String displayName, @Nullable String toolsExtName) {
        this.name = name;
        this.displayName = displayName;
        this.toolsExtName = toolsExtName;

        this.problemsInternal = this.getObjects().newInstance(EnhancedProblems.Minimal.class, name, displayName);
    }

    /// This method is used by Gradle to apply this plugin. You should instead override [#setup(T)] to do plugin setup.
    ///
    /// @param target The target for this plugin
    @Override
    public final void apply(T target) {
        this.setup(this.target = target);

        if (this.toolsExtName != null && target instanceof ExtensionAware)
            this.tools = ((ExtensionAware) target).getExtensions().create(this.toolsExtName, ToolsExtensionImpl.class, (Callable<? extends JavaToolchainService>) this::toolchainsForTools);
//        else
//            this.tools = this.getObjects().newInstance(ToolsExtensionImpl.class, (Callable<? extends JavaToolchainService>) this::toolchainsForTools);
    }

    /// Called when this plugin is applied to do setup work.
    ///
    /// @param target The target for this plugin (can also get after setup with [#getTarget()])
    public abstract void setup(T target);

    /// Gets the target for this plugin. This will throw an exception if this is called before application (i.e. through
    /// early usage of [#globalCaches()]).
    ///
    /// @return The plugin target
    /// @throws RuntimeException If this plugin is not yet applied
    private T getTarget() {
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

    @Override
    public Tool.Resolved getTool(Tool tool) {
        ProviderFactory providers;
        try {
            providers = this.target instanceof Project ? this.getProviders() : ((Gradle) InvokerHelper.getProperty(this.target, "gradle")).getRootProject().getProviders();
        } catch (Throwable ignored) {
            providers = this.getProviders();
        }

        return ((ToolInternal) tool).get(this.globalCaches(), providers, this.tools);
    }

    // NOTE: Use this in Tool implementations. Enhanced plugins do not enforce application on projects.
    JavaToolchainService toolchainsForTools() {
        return this.target instanceof Project ? this.getJavaToolchains() : ((Gradle) InvokerHelper.getProperty(this.target, "gradle")).getRootProject().getExtensions().getByType(JavaToolchainService.class);
    }


    /* CACHES */

    private final Lazy<DirectoryProperty> globalCaches = Lazy.simple(this::makeGlobalCaches);

    @Override
    public final DirectoryProperty globalCaches() {
        return this.globalCaches.get();
    }

    private DirectoryProperty makeGlobalCaches() {
        try {
            Gradle gradle = ((Gradle) InvokerHelper.getProperty(this.target, "gradle"));
            DirectoryProperty gradleUserHomeDir = this.getObjects().directoryProperty().fileValue(gradle.getGradleUserHomeDir());

            return this.getObjects().directoryProperty().convention(
                gradleUserHomeDir.dir("caches/minecraftforge/" + this.name).map(this.problemsInternal.ensureFileLocation())
            );
        } catch (Exception e) {
            throw this.problemsInternal.illegalPluginTarget(
                new IllegalArgumentException(String.format("Failed to get %s global caches directory for target: %s", this.displayName, this.target), e),
                "types with access to Gradle via `#getGradle()`"
            );
        }
    }

    private final Lazy<DirectoryProperty> localCaches = Lazy.simple(this::makeLocalCaches);

    @Override
    public final DirectoryProperty localCaches() {
        return this.localCaches.get();
    }

    private DirectoryProperty makeLocalCaches() {
        try {
            DirectoryProperty workingProjectBuildDir;
            if (this.target instanceof Project) {
                workingProjectBuildDir = this.getProjectLayout().getBuildDirectory();
            } else if (this.target instanceof Settings) {
                workingProjectBuildDir = this.getObjects().directoryProperty().fileValue(new File(this.getBuildLayout().getRootDirectory().getAsFile(), "build"));
            } else {
                throw new IllegalStateException("Cannot make local caches with an unsupported type (must be project or settings)");
            }

            return this.getObjects().directoryProperty().convention(
                workingProjectBuildDir.dir("minecraftforge/" + this.name).map(this.problemsInternal.ensureFileLocation())
            );
        } catch (Exception e) {
            throw this.problemsInternal.illegalPluginTarget(
                new IllegalArgumentException(String.format("Failed to get %s local caches directory for target: %s", this.displayName, this.getTarget()), e),
                Project.class, Settings.class
            );
        }
    }

    private final Lazy<DirectoryProperty> workingProjectDirectory = Lazy.simple(this::makeWorkingProjectDirectory);

    @Override
    public final DirectoryProperty workingProjectDirectory() {
        return this.workingProjectDirectory.get();
    }

    private DirectoryProperty makeWorkingProjectDirectory() {
        try {
            DirectoryProperty workingProjectDirectory = this.getObjects().directoryProperty();
            if (this.target instanceof Project) {
                return workingProjectDirectory.value(this.getProjectLayout().getProjectDirectory());
            } else if (this.target instanceof Settings) {
                return workingProjectDirectory.value(this.getBuildLayout().getRootDirectory());
            } else {
                throw new IllegalStateException("Cannot get working project directory with an unsupported type (must be project or settings)");
            }
        } catch (Exception e) {
            throw this.problemsInternal.illegalPluginTarget(
                new IllegalArgumentException(String.format("Failed to get %s working project directory for target: %s", this.displayName, this.getTarget()), e),
                Project.class, Settings.class
            );
        }
    }
}
