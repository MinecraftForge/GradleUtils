package net.minecraftforge.gradleutils.shared;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyFactory;
import org.gradle.api.artifacts.dsl.ExternalModuleDependencyVariantSpec;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderConvertible;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Console;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;

public abstract non-sealed class ToolExecSpec implements EnhancedTaskAdditions {
    //region JavaExec
    public abstract @InputFiles @Classpath ConfigurableFileCollection getClasspath();

    public abstract @Input @Optional Property<String> getMainClass();

    public abstract @Nested Property<JavaLauncher> getJavaLauncher();

    protected abstract @Nested Property<JavaLauncher> getToolchainLauncher();

    public abstract @Input @Optional Property<Boolean> getPreferToolchainJvm();

    public abstract @Internal DirectoryProperty getWorkingDir();

    protected abstract @Internal MapProperty<String, String> getForkProperties();
    //endregion

    //region Logging
    protected abstract @Console Property<LogLevel> getStandardOutputLogLevel();

    protected abstract @Console Property<LogLevel> getStandardErrorLogLevel();

    protected abstract @Internal RegularFileProperty getLogFile();
    //endregion

    @Override
    public EnhancedPlugin<?> plugin() {
        return this.plugin;
    }

    protected abstract @Inject Project getProject();

    protected abstract @Inject ObjectFactory getObjects();

    protected abstract @Inject ProviderFactory getProviders();

    protected abstract @Inject ExecOperations getExecOperations();

    protected abstract @Inject DependencyFactory getDependencyFactory();

    protected abstract @Inject JavaToolchainService getJavaToolchains();

    private final transient EnhancedPlugin<?> plugin;
    private final String toolName;

    @Inject
    public ToolExecSpec(EnhancedPlugin<?> plugin, Tool.Resolved resolved) {
        this.plugin = plugin;
        this.toolName = resolved.getName();
        this.getClasspath().setFrom(resolved.getClasspath());

        if (resolved.hasMainClass())
            this.getMainClass().set(resolved.getMainClass());
        this.getJavaLauncher().set(resolved.getJavaLauncher());

        try {
            this.getToolchainLauncher().convention(getJavaToolchains().launcherFor(spec -> spec.getLanguageVersion().set(JavaLanguageVersion.current())));
            getProject().getPluginManager().withPlugin("java", javaAppliedPlugin ->
                this.getToolchainLauncher().set(getJavaToolchains().launcherFor(getProject().getExtensions().getByType(JavaPluginExtension.class).getToolchain()))
            );
        } catch (Exception ignored) {
            // If these fail, we're likely in a Settings environment
            // This is fine, we can just try using the Daemon JVM
        }

        this.getForkProperties().set(SharedUtil.getForkProperties(getProviders()));

        this.getStandardOutputLogLevel().convention(LogLevel.LIFECYCLE);
        this.getStandardErrorLogLevel().convention(LogLevel.ERROR);

        this.getWorkingDir().convention(this.getDefaultOutputDirectory());
        this.getLogFile().convention(this.getDefaultLogFile());
    }

    public final void using(CharSequence dependency) {
        this.using(getDependencyFactory().create(dependency));
    }

    public final void using(Provider<? extends Dependency> dependency) {
        this.getClasspath().setFrom(
            getProject().getConfigurations().detachedConfiguration().withDependencies(d -> d.addLater(dependency))
        );
    }

    public final void using(Provider<MinimalExternalModuleDependency> dependency, Action<? super ExternalModuleDependencyVariantSpec> variantSpec) {
        this.using(getProject().getDependencies().variantOf(dependency, variantSpec));
    }

    public final void using(ProviderConvertible<? extends Dependency> dependency) {
        this.using(dependency.asProvider());
    }

    public final void using(ProviderConvertible<MinimalExternalModuleDependency> dependency, Action<? super ExternalModuleDependencyVariantSpec> variantSpec) {
        this.using(getProject().getDependencies().variantOf(dependency, variantSpec));
    }

    public final void using(Dependency dependency) {
        this.getClasspath().setFrom(
            getProject().getConfigurations().detachedConfiguration(dependency)
        );
    }

    @Deprecated
    public final void usingDirectly(CharSequence downloadUrl) {
        var url = getProviders().provider(downloadUrl::toString);
        this.getClasspath().setFrom(getProviders().of(ToolImpl.Source.class, spec -> spec.parameters(parameters -> {
            parameters.getInputFile().set(getProviders().zip(localCaches(), url, (d, s) -> d.file("tools/" + toolName + '/' + s.substring(s.lastIndexOf('/')))));
            parameters.getDownloadUrl().set(url);
        })));
    }

    private <T extends FileSystemLocation> Transformer<T, T> ensureFileLocationInternal() {
        return t -> this.plugin.getProblemsInternal().<T>ensureFileLocation().transform(t);
    }
}
