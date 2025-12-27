/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FirstParam;
import kotlin.jvm.functions.Function0;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.TaskExecutionRequest;
import org.gradle.api.Action;
import org.gradle.api.DomainObjectCollection;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.Project;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderConvertible;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// Shared utilities for Gradle plugins.
///
/// @implNote Consumers should make their own `Util` class and extend this one with it to inherit all static methods.
public abstract class SharedUtil {
    //region Java Launcher

    /// Transformer to map a Java launcher to its executable path. Use to store in properties since [JavaLauncher]
    /// cannot be serialized.
    public static final Transformer<String, JavaLauncher> LAUNCHER_EXECUTABLE = it -> it.getExecutablePath().toString();

    /// Gets the Java launcher that [can compile or run][JavaLanguageVersion#canCompileOrRun(JavaLanguageVersion)] the
    /// given version.
    ///
    /// If the currently available Java toolchain is able to compile and run the given version, it will be used instead.
    /// The toolchain is first queried from the project's [JavaPluginExtension#getToolchain()]. If the toolchain is not
    /// set or does not apply the `java` plugin, [JavaLanguageVersion#current()] will be used instead.
    ///
    /// @param project The project to get the Java extensions from
    /// @param version The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherFor(Project project, int version) {
        return launcherFor(project, JavaLanguageVersion.of(version));
    }

    /// Gets the Java launcher that [can compile or run][JavaLanguageVersion#canCompileOrRun(JavaLanguageVersion)] the
    /// given version.
    ///
    /// If the currently available Java toolchain is able to compile and run the given version, it will be used instead.
    /// The toolchain is first queried from the project's [JavaPluginExtension#getToolchain()]. If the toolchain is not
    /// set or does not apply the `java` plugin, [JavaLanguageVersion#current()] will be used instead.
    ///
    /// @param project The project to get the Java extensions from
    /// @param version The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherFor(Project project, JavaLanguageVersion version) {
        ProviderFactory providers = project.getProviders();
        ExtensionContainer extensions = project.getExtensions();
        return launcherFor(providers.provider(() -> extensions.findByType(JavaPluginExtension.class)), getJavaToolchainService(project), version);
    }

    private static Provider<JavaLauncher> launcherFor(Provider<? extends JavaPluginExtension> java, JavaToolchainService javaToolchains, int version) {
        return launcherFor(java, javaToolchains, JavaLanguageVersion.of(version));
    }

    private static Provider<JavaLauncher> launcherFor(Provider<? extends JavaPluginExtension> java, JavaToolchainService javaToolchains, JavaLanguageVersion version) {
        return java.flatMap(j -> launcherFor(j, javaToolchains, version)).orElse(launcherFor(javaToolchains, version));
    }

    private static Provider<JavaLauncher> launcherFor(JavaPluginExtension java, JavaToolchainService javaToolchains, int version) {
        return launcherFor(java, javaToolchains, JavaLanguageVersion.of(version));
    }

    private static Provider<JavaLauncher> launcherFor(JavaPluginExtension java, JavaToolchainService javaToolchains, JavaLanguageVersion version) {
        return java.getToolchain().getLanguageVersion().orElse(JavaLanguageVersion.current()).flatMap(
            languageVersion -> languageVersion.canCompileOrRun(version)
                ? javaToolchains.launcherFor(spec -> spec.getLanguageVersion().set(languageVersion))
                : launcherForStrictly(javaToolchains, version)
        );
    }

    private static Provider<JavaLauncher> launcherFor(JavaToolchainService javaToolchains, int version) {
        return launcherFor(javaToolchains, JavaLanguageVersion.of(version));
    }

    private static Provider<JavaLauncher> launcherFor(JavaToolchainService javaToolchains, JavaLanguageVersion version) {
        JavaLanguageVersion languageVersion = JavaLanguageVersion.current();
        return languageVersion.canCompileOrRun(version)
            ? javaToolchains.launcherFor(spec -> spec.getLanguageVersion().set(languageVersion))
            : launcherForStrictly(javaToolchains, version);
    }

    /// Gets the Java launcher for the given version, even if the currently running Java toolchain is higher.
    ///
    /// @param project The extension-aware object to get the Java extensions from
    /// @param version The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherForStrictly(Project project, int version) {
        return launcherForStrictly(getJavaToolchainService(project), version);
    }

    /// Gets the Java launcher for the given version, even if the currently running Java toolchain is higher.
    ///
    /// @param project The extension-aware object to get the Java extensions from
    /// @param version The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherForStrictly(Project project, JavaLanguageVersion version) {
        return launcherForStrictly(getJavaToolchainService(project), version);
    }

    /// Gets the Java launcher for the given version, even if the currently running Java toolchain is higher.
    ///
    /// @param javaToolchains The Java toolchain service to get the Java launcher from
    /// @param version        The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherForStrictly(JavaToolchainService javaToolchains, int version) {
        return launcherForStrictly(javaToolchains, JavaLanguageVersion.of(version));
    }

    /// Gets the Java launcher for the given version, even if the currently running Java toolchain is higher.
    ///
    /// @param javaToolchains The Java toolchain service to get the Java launcher from
    /// @param version        The version of Java required
    /// @return A provider for the Java launcher
    public static Provider<JavaLauncher> launcherForStrictly(JavaToolchainService javaToolchains, JavaLanguageVersion version) {
        return javaToolchains.launcherFor(spec -> spec.getLanguageVersion().set(version));
    }

    private static JavaToolchainService getJavaToolchainService(Project project) {
        return project.getObjects().newInstance(ProjectServiceWrapper.class).getJavaToolchains();
    }

    static abstract class ProjectServiceWrapper {
        protected abstract @Inject JavaToolchainService getJavaToolchains();

        @Inject
        public ProjectServiceWrapper() { }
    }

    public static Provider<Map<String, String>> getForkProperties(ProviderFactory providers) {
        return providers.of(JavaForkProperties.class, spec -> { });
    }
    //endregion

    //region Project Eval

    /// Runs the given closure using [Project#afterEvaluate(Action)]. If the project is already executed, the closure
    /// will be called instantly.
    ///
    /// @param project The project to run the closure on
    /// @param closure The closure to execute
    public static void ensureAfterEvaluate(
        Project project,
        @DelegatesTo(value = Project.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FirstParam.class)
        Closure<?> closure
    ) {
        ensureAfterEvaluate(project, Closures.toAction(closure));
    }

    /// Runs the given action using [Project#afterEvaluate(Action)]. If the project is already executed, the action will
    /// be executed instantly.
    ///
    /// @param project The project to run the action on
    /// @param action  The action to execute
    public static void ensureAfterEvaluate(Project project, Action<? super Project> action) {
        if (project.getState().getExecuted())
            action.execute(project);
        else
            project.afterEvaluate(action);
    }

    /// Ensures that a given task is run first in the task graph for the given project.
    ///
    /// This *does not* break the configuration cache as long as the task is always applied using this.
    ///
    /// @param <T>     The type of task to be run
    /// @param project The project
    /// @param task    The provider of the task to run first
    /// @return The task provider
    public static <T extends TaskProvider<?>> T runFirst(Project project, T task) {
        // copy the requests because the backed list isn't concurrent
        var requests = new ArrayList<>(project.getGradle().getStartParameter().getTaskRequests());

        // add the task to the front of the list
        requests.add(0, new TaskExecutionRequest() {
            @Override
            public List<String> getArgs() {
                return List.of(task.get().getPath());
            }

            @Override
            public @Nullable String getProjectPath() {
                return null;
            }

            @Override
            public @Nullable File getRootDir() {
                return null;
            }
        });

        // set the new requests
        project.getLogger().info("Adding task to beginning of task graph! Project: {}, Task: {}", project.getName(), task.getName());
        project.getGradle().getStartParameter().setTaskRequests(requests);
        return task;
    }
    //endregion

    //region Dependency Information

    public record SimpleModuleIdentifier(String getGroup, String getName) implements ModuleIdentifier {
        static SimpleModuleIdentifier of(String group, String name) {
            return new SimpleModuleIdentifier(group, name);
        }

        static SimpleModuleIdentifier of(String module) {
            var substring = module.split(":");
            if (substring.length != 2)
                throw new IllegalArgumentException("Invalid non-versioned module identifier: " + module);

            return of(substring[0], substring[1]);
        }
    }

    public record SimpleModuleVersionIdentifier(ModuleIdentifier getModule, String getVersion,
                                                @Nullable String classifier,
                                                String extension) implements ModuleVersionIdentifier {
        @Override
        public String getGroup() {
            return getModule.getGroup();
        }

        @Override
        public String getName() {
            return getModule.getName();
        }

        public String getDownloadUrl(String prefix) {
            var builder = new StringBuilder();

            // Use HTTPS by default if protocol not defined
            if (!prefix.contains("://"))
                builder.append("https://");

            builder.append(prefix);

            // Account for trailing slash
            if (!prefix.endsWith("/"))
                builder.append('/');

            builder.append(getGroup().replace('.', '/'))
                   .append('/').append(getName())
                   .append('/').append(getVersion())
                   .append('/').append(getFileName());

            return builder.toString();
        }

        public String getFileName() {
            var builder = new StringBuilder()
                .append(getName())
                .append('-')
                .append(getVersion());

            if (classifier != null)
                builder.append('-').append(classifier);

            return builder.append('.').append(extension).toString();
        }

        static SimpleModuleVersionIdentifier of(ModuleIdentifier module, String version) {
            return of(module, version, null, "jar");
        }

        static SimpleModuleVersionIdentifier of(ModuleIdentifier module, String version, @Nullable String classifier, String extension) {
            return new SimpleModuleVersionIdentifier(module, version, classifier, extension);
        }

        static SimpleModuleVersionIdentifier of(String module, String version) {
            return of(SimpleModuleIdentifier.of(module), version);
        }

        static SimpleModuleVersionIdentifier of(String group, String name, String version) {
            return of(SimpleModuleIdentifier.of(group, name), version);
        }

        static SimpleModuleVersionIdentifier of(String group, String name, String version, @Nullable String classifier, String extension) {
            return of(SimpleModuleIdentifier.of(group, name), version, classifier, extension);
        }

        static SimpleModuleVersionIdentifier of(String artifact) {
            var split = artifact.split(":", 4);
            var group = split[0];
            var name = split[1];

            String version;
            @Nullable String classifier = null;
            String extension = "jar";

            // Check if version has @ before :
            if (split[2].indexOf('@') > 0) {
                if (split.length > 3)
                    throw new IllegalArgumentException("Invalid module version identifier (found @ character before another : character): " + artifact);

                var s = split[2].split("@");
                version = s[0];
                extension = s[1];
            } else {
                version = split[2];
            }

            // Check if classifier has an @
            if (split.length > 3) {
                if (split[3].indexOf('@') > 0) {
                    var s = split[2].split("@");
                    classifier = s[0];
                    extension = s[1];
                } else {
                    classifier = split[3];
                }
            }

            return of(group, name, version, classifier, extension);
        }

        public SimpleModuleVersionIdentifier withVersion(String version) {
            return of(getModule(), version, classifier(), extension());
        }

        public SimpleModuleVersionIdentifier withClassifier(String classifier) {
            return of(getModule(), getVersion(), classifier, extension());
        }

        public SimpleModuleVersionIdentifier withExtension(String extension) {
            return of(getModule(), getVersion(), classifier(), extension);
        }

        @Override
        public String toString() {
            var builder = new StringBuilder()
                .append(getGroup())
                .append(':').append(getName())
                .append(':').append(getVersion());

            if (classifier != null)
                builder.append(':').append(classifier);

            if ("jar".equals(extension))
                builder.append('@').append(extension);

            return builder.toString();
        }
    }

    public static SimpleModuleVersionIdentifier moduleOf(String artifact) {
        return SimpleModuleVersionIdentifier.of(artifact);
    }

    public static SimpleModuleIdentifier moduleOf(String group, String name) {
        return SimpleModuleIdentifier.of(group, name);
    }

    public static SimpleModuleVersionIdentifier moduleOf(String group, String name, String version) {
        return SimpleModuleVersionIdentifier.of(group, name, version);
    }

    public static SimpleModuleVersionIdentifier moduleOf(ModuleIdentifier module, String version) {
        return SimpleModuleVersionIdentifier.of(module, version);
    }

    public static String dependencyToArtifactString(Dependency dependency) {
        var builder = new StringBuilder();

        builder.append(dependency.getGroup() != null ? dependency.getGroup() + ':' : "");
        builder.append(dependency.getName());
        builder.append(dependency.getVersion() != null ? ':' + dependency.getVersion() : "");
        var classifier = getProperty(dependency, "classifier");
        builder.append(classifier != null ? ':' + classifier : "");
        var extension = getProperty(dependency, "extension", "artifactType");
        builder.append(extension != null ? '@' + extension : "");

        return builder.toString();
    }

    private static @Nullable String getProperty(Object object, String... property) {
        for (var name : property) {
            var p = DefaultGroovyMethods.hasProperty(object, name);
            if (p == null) continue;

            var o = p.getProperty(object);
            if (o == null) continue;

            var s = o.toString();
            return !"null".equals(s) ? s : null;
        }

        return null;
    }
    //endregion

    //region Dependency Handling

    /// Checks if the given dependency is in the given source set.
    ///
    /// @param configurations The configuration container to use
    /// @param sourceSet      The source set to check
    /// @param transitive     If the source set should be searched transitively (if `false`, for example, this method
    ///                       will return `false` if the dependency is in the `main` source set but not explicitely
    ///                       declared in one of the `test` source set's dependency-scope configurations)
    /// @param dependency     The dependency to find
    /// @return If the source set contains the dependency
    public static boolean contains(NamedDomainObjectSet<Configuration> configurations, SourceSet sourceSet, boolean transitive, Dependency dependency) {
        return contains(configurations, sourceSet, transitive, dependency::equals);
    }

    /// Checks if the given dependency is in the given source set.
    ///
    /// @param configurations The configuration container to use
    /// @param sourceSet      The source set to check
    /// @param transitive     If the source set should be searched transitively (if `false`, for example, this method
    ///                       will return `false` if the dependency is in the `main` source set but not explicitely
    ///                       declared in one of the `test` source set's dependency-scope configurations)
    /// @param dependency     The dependency to find
    /// @return If the source set contains the dependency
    public static boolean contains(NamedDomainObjectSet<Configuration> configurations, SourceSet sourceSet, boolean transitive, Spec<? super Dependency> dependency) {
        return contains(configurations, sourceSet.getCompileOnlyConfigurationName(), transitive, dependency) ||
            contains(configurations, sourceSet.getCompileOnlyApiConfigurationName(), transitive, dependency) ||
            contains(configurations, sourceSet.getCompileClasspathConfigurationName(), transitive, dependency) ||
            contains(configurations, sourceSet.getAnnotationProcessorConfigurationName(), transitive, dependency) ||
            contains(configurations, sourceSet.getApiConfigurationName(), transitive, dependency) ||
            contains(configurations, sourceSet.getImplementationConfigurationName(), transitive, dependency) ||
            contains(configurations, sourceSet.getApiElementsConfigurationName(), transitive, dependency) ||
            contains(configurations, sourceSet.getRuntimeOnlyConfigurationName(), transitive, dependency) ||
            contains(configurations, sourceSet.getRuntimeClasspathConfigurationName(), transitive, dependency) ||
            contains(configurations, sourceSet.getRuntimeElementsConfigurationName(), transitive, dependency);
    }

    private static boolean contains(NamedDomainObjectSet<Configuration> configurations, String configurationName, boolean transitive, Spec<? super Dependency> dependency) {
        var configuration = configurations.findByName(configurationName);
        return configuration != null && !(transitive ? configuration.getAllDependencies() : configuration.getDependencies()).matching(dependency).isEmpty();
    }

    /// Checks if the given dependency is in the given source set.
    ///
    /// @param configurations The configuration container to use
    /// @param sourceSet      The source set to check
    /// @param transitive     If the source set should be searched transitively (if `false`, for example, this method
    ///                       will return `false` if the dependency is in the `main` source set but not explicitely
    ///                       declared in one of the `test` source set's dependency-scope configurations)
    /// @param dependency     The dependency to find
    /// @return The set containing the filtered dependencies
    public static Set<Dependency> collect(NamedDomainObjectSet<Configuration> configurations, SourceSet sourceSet, boolean transitive, Predicate<? super Dependency> dependency) {
        return Stream
            .of(
                configurations.findByName(sourceSet.getCompileOnlyConfigurationName()),
                configurations.findByName(sourceSet.getCompileOnlyApiConfigurationName()),
                configurations.findByName(sourceSet.getCompileClasspathConfigurationName()),
                configurations.findByName(sourceSet.getAnnotationProcessorConfigurationName()),
                configurations.findByName(sourceSet.getApiConfigurationName()),
                configurations.findByName(sourceSet.getImplementationConfigurationName()),
                configurations.findByName(sourceSet.getApiElementsConfigurationName()),
                configurations.findByName(sourceSet.getRuntimeOnlyConfigurationName()),
                configurations.findByName(sourceSet.getRuntimeClasspathConfigurationName()),
                configurations.findByName(sourceSet.getRuntimeElementsConfigurationName())
            )
            .filter(Objects::nonNull)
            .flatMap(configuration -> transitive ? configuration.getAllDependencies().stream() : configuration.getDependencies().stream())
            .distinct()
            .filter(dependency)
            .collect(Collectors.toSet());
    }

    public static NamedDomainObjectSet<SourceSet> collect(Project project, boolean transitive, Dependency dependency) {
        return collect(project, transitive, dependency::equals);
    }

    public static NamedDomainObjectSet<SourceSet> collect(Project project, boolean transitive, Spec<? super Dependency> dependency) {
        return collect(
            project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets(),
            project.getConfigurations(),
            transitive,
            dependency
        );
    }

    public static NamedDomainObjectSet<SourceSet> collect(NamedDomainObjectContainer<SourceSet> sourceSets, NamedDomainObjectSet<Configuration> configurations, boolean transitive, Dependency dependency) {
        return collect(sourceSets, configurations, transitive, dependency::equals);
    }

    public static NamedDomainObjectSet<SourceSet> collect(NamedDomainObjectContainer<SourceSet> sourceSets, NamedDomainObjectSet<Configuration> configurations, boolean transitive, Spec<? super Dependency> dependency) {
        return sourceSets.matching(sourceSet -> {
            var candidates = configurations.named(name ->
                // Always check these resolvable configurations
                name.equals(sourceSet.getCompileClasspathConfigurationName())
                    || name.equals(sourceSet.getRuntimeClasspathConfigurationName())
                    || name.equals(sourceSet.getAnnotationProcessorConfigurationName())

                    // If not checking transitively, we need to check these declared configurations as well
                    || (!transitive && (
                    name.equals(sourceSet.getCompileOnlyConfigurationName())
                        || name.equals(sourceSet.getCompileOnlyApiConfigurationName())
                        || name.equals(sourceSet.getRuntimeOnlyConfigurationName())
                        || name.equals(sourceSet.getImplementationConfigurationName())
                        || name.equals(sourceSet.getApiConfigurationName())
                ))
            );

            // The candidate matches if the dependency set matches our dependency spec
            return !candidates.matching(configuration -> {
                var dependencies = transitive ? configuration.getAllDependencies() : configuration.getDependencies();
                return !dependencies.matching(dependency).isEmpty();
            }).isEmpty();
        });
    }

    static <T> void guardCheck(T t) { }

    /// Iterates through the given source set's classpath configurations using the given action.
    ///
    /// @param configurations The configuration container
    /// @param sourceSet      The source set
    /// @param action         The action to run
    /// @see #forEach(DomainObjectCollection, Action)
    public static void forEachClasspath(NamedDomainObjectSet<Configuration> configurations, SourceSet sourceSet, Action<? super Configuration> action) {
        forEach(configurations.named(
            name -> name.equals(sourceSet.getCompileClasspathConfigurationName())
                || name.equals(sourceSet.getRuntimeClasspathConfigurationName())
        ), action);
    }

    /// Iterates through the given source set's classpath configurations eagerly using the given action.
    ///
    /// @param configurations The configuration container
    /// @param sourceSet      The source set
    /// @param action         The action to run
    /// @see #forEachEagerly(DomainObjectCollection, Action)
    public static void forEachClasspathEagerly(NamedDomainObjectSet<Configuration> configurations, SourceSet sourceSet, Action<? super Configuration> action) {
        forEachEagerly(configurations.named(
            name -> name.equals(sourceSet.getCompileClasspathConfigurationName())
                || name.equals(sourceSet.getRuntimeClasspathConfigurationName())
        ), action);
    }
    //endregion

    //region Dependency Versioning

    public static int versionCompare(String v1, String v2) {
        return StaticVersionComparator.compareNow(v1, v2);
    }

    public static Comparator<String> versionComparator() {
        return StaticVersionComparator.INSTANCE;
    }

    public static Class<? extends Comparator<String>> versionComparatorClass() {
        return StaticVersionComparator.class;
    }
    //endregion

    //region Domain Object Handling

    /// Iterates through the given collection using the given action.
    ///
    /// This iterator will attempt to use [DomainObjectCollection#configureEach(Action)] if it is in an eager context.
    /// If it is not, a [copy of][List#copyOf(Collection)] the collection will be iterated through using
    /// [List#forEach(Consumer)] instead to prevent a [java.util.ConcurrentModificationException].
    ///
    /// @param <T>        The type for the collection
    /// @param collection The collection to iterate through
    /// @param action     The action to run
    public static <T> void forEach(DomainObjectCollection<T> collection, Action<? super T> action) {
        boolean eager = false;
        try {
            collection.configureEach(SharedUtil::guardCheck);
        } catch (IllegalStateException e) {
            eager = true;
        }

        if (eager) {
            List.copyOf(collection).forEach(action::execute);
        } else {
            collection.configureEach(action);
        }
    }

    /// Iterates through the given collection eagerly using the given action.
    ///
    /// This iterator will iterate over a [copy of][List#copyOf(Collection)] the collection using
    /// [List#forEach(Consumer)] to prevent a [java.util.ConcurrentModificationException].
    ///
    /// @param <T>        The type for the collection
    /// @param collection The collection to iterate through
    /// @param action     The action to run
    public static <T> void forEachEagerly(DomainObjectCollection<T> collection, Action<? super T> action) {
        List.copyOf(collection).forEach(action::execute);
    }
    //endregion

    //region Action Logging

    /// Creates an output stream that logs to the given action.
    ///
    /// @param logger The logger to log to
    /// @param level  The log level to log at
    /// @return The output stream
    public static PipedOutputStream toLog(Logger logger, LogLevel level) {
        return toLog(s -> logger.log(level, s));
    }

    /// Creates an output stream that logs to the given action.
    ///
    /// @param logger The logger to log to
    /// @return The output stream
    public static PipedOutputStream toLog(Consumer<? super String> logger) {
        final PipedOutputStream output;
        final PipedInputStream input;
        try {
            output = new PipedOutputStream();
            input = new PipedInputStream(output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                reader.lines().forEach(logger);
            } catch (IOException ignored) { }
        });

        thread.setDaemon(true);
        thread.start();

        return output;
    }
    //endregion

    //region toString()

    /// Converts a given module to string. Use this instead of [Object#toString()].
    ///
    /// @param module The module
    /// @return The string representation
    public static String toString(ModuleVersionSelector module) {
        String version = module.getVersion();
        return String.format("%s:%s%s",
            module.getGroup(),
            module.getName(),
            version != null ? ':' + version : ""
        );
    }

    /// Converts a given dependency to string. Use this instead of [Object#toString()].
    ///
    /// @param dependency The dependency
    /// @return The string representation
    public static String toString(Dependency dependency) {
        String group = dependency.getGroup();
        String version = dependency.getVersion();
        String reason = dependency.getReason();
        return String.format("(%s) %s%s%s%s%s",
            dependency.getClass().getName(),
            group != null ? group + ':' : "",
            dependency.getName(),
            version != null ? ':' + version : "",
            reason != null ? " (" + reason + ')' : "",
            dependency instanceof FileCollectionDependency ? String.format(" [%s]", String.join(", ", ((FileCollectionDependency) dependency).getFiles().getFiles().stream().map(File::getAbsolutePath).map(CharSequence.class::cast)::iterator)) : ""
        );
    }

    /// Converts a given dependency to a relative path, accounting for the possible nullable
    /// {@linkplain Dependency#getGroup() group} and {@linkplain Dependency#getVersion() version}.
    ///
    /// @param dependency The dependency to pathify
    /// @return The pathified dependency
    public static String pathify(Dependency dependency) {
        var group = dependency.getGroup();
        var name = dependency.getName();
        var version = dependency.getVersion();
        return MessageFormat.format("{0}{1}/{2}",
            group == null ? "" : group.replace('.', '/') + '/',
            name,
            version == null ? "/" : '/' + version + '/'
        );
    }

    //endregion

    //region Deferred Objects

    /// Unpacks a deferred value.
    ///
    /// Since buildscripts are dynamically compiled, this allows buildscript authors to use this method with version
    /// catalog entries, other provider-like objects. This prevents the need to arbitrarily call [Provider#get()] (or
    /// similar) on values which may or may not be deferred based on circumstance.
    ///
    /// @param value The value to unpack
    /// @param <T>   The type of value held by the provider
    /// @return The unpacked value
    @SuppressWarnings("unchecked")
    public static <T> T unpack(Object value) {
        if (value instanceof ProviderConvertible<?> deferred) {
            return (T) deferred.asProvider().get();
        } else if (value instanceof Provider<?> deferred) {
            return (T) deferred.get();
        } else if (value instanceof Closure<?> deferred) {
            return Closures.invoke(deferred);
        } else if (value instanceof Callable<?> deferred) {
            try {
                return (T) deferred.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (value instanceof Function0<?> deferred) {
            return (T) deferred.invoke();
        } else if (value instanceof Supplier<?> deferred) {
            return (T) deferred.get();
        } else {
            return (T) value;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Provider<T> asProvider(ProviderFactory providers, Object value) {
        if (value instanceof ProviderConvertible<?> deferred) {
            return (Provider<T>) deferred.asProvider();
        } else if (value instanceof Provider<?> deferred) {
            return (Provider<T>) deferred;
        } else if (value instanceof Closure<?> deferred) {
            return providers.provider(() -> Closures.invoke(deferred));
        } else if (value instanceof Callable<?> deferred) {
            return providers.provider((Callable<T>) deferred);
        } else if (value instanceof Function0<?> deferred) {
            return providers.provider(() -> (T) deferred.invoke());
        } else if (value instanceof Supplier<?> deferred) {
            return providers.provider(() -> (T) deferred.get());
        } else {
            return providers.provider(() -> (T) value);
        }
    }
    //endregion

    //region Gradle Properties

    /**
     * Makes a returning-self closure that finalizes a given property using {@link #finalizeProperty(Property)}.
     * <p>This is best used as the method argument for
     * {@link org.codehaus.groovy.runtime.DefaultGroovyMethods#tap(Object, Closure)}, which allows for in-lining
     * property creation in Groovy code.</p>
     *
     * @param <P> The type of property to finalize
     * @return The returning-self closure for finalizing a property
     */
    public static <P extends Property<?>> Closure<P> finalizeProperty() {
        Closure<P> ret = Closures.unaryOperator(SharedUtil::finalizeProperty);
        ret.setResolveStrategy(Closure.DELEGATE_FIRST);
        return ret;
    }

    /// Finalizes the given property to prevent any additional changes from being made to it.
    ///
    /// This is done by simply calling [Property#disallowChanges()] and [Property#finalizeValueOnRead()]. These methods
    /// do not return the object itself, so this helper method exists to in-line property creation without needing to
    /// reference it again just to call these methods.
    ///
    /// @param <P>      The type of property to finalize
    /// @param property The property to finalize
    /// @return The property
    @Contract(value = "_ -> param1", mutates = "param1")
    public static <P extends Property<?>> P finalizeProperty(P property) {
        property.disallowChanges();
        property.finalizeValueOnRead();
        return property;
    }

    /// Conditionally set the given provider's value to the given property's value if the property is
    /// [present][Provider#isPresent()].
    ///
    /// @param from The provider value to apply
    /// @param to   The property to apply the new value to
    /// @param <T>  The type of property
    public static <T> void setOptional(Property<T> to, Provider<? extends T> from) {
        if (from.isPresent()) to.set(from);
    }
    //endregion

    /// Empty constructor. This class should only be extended to make referencing these static methods easier.
    protected SharedUtil() { }
}
