/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import net.minecraftforge.util.download.DownloadUtils;
import net.minecraftforge.util.hash.HashStore;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;

record ToolImpl(
    String getName,
    ModuleVersionIdentifier getModule,
    String artifact,
    String fileName,
    String downloadUrl,
    int getJavaVersion,
    @Nullable String getMainClass,
    String mavenUrl
) implements ToolInternal {
    private static final @Serial long serialVersionUID = -862411638019629688L;

    private static final Logger LOGGER = Logging.getLogger(Tool.class);

    ToolImpl(String name, String artifact, String mavenUrl, int javaVersion, @Nullable String mainClass) {
        this(name, SharedUtil.moduleOf(artifact), artifact, mavenUrl, javaVersion, mainClass);
    }

    ToolImpl(String name, SharedUtil.SimpleModuleVersionIdentifier module, String artifact, String mavenUrl, int javaVersion, @Nullable String mainClass) {
        this(
            name,
            module,
            artifact,
            module.getFileName(),
            module.getDownloadUrl(mavenUrl),
            javaVersion,
            mainClass,
            mavenUrl
        );
    }

    private record Overrides(String downloadUrl, String fileName, String artifact){}

    private Overrides fillOverrides(Tool.Definition definition) {
        var downloadUrl = this.downloadUrl;
        var fileName = this.fileName;
        var artifact = this.artifact;

        if (definition.getArtifact().isPresent()) {
            artifact = definition.getArtifact().get();
            var parsed = SharedUtil.moduleOf(artifact);
            downloadUrl = parsed.getDownloadUrl(this.mavenUrl);
            fileName = parsed.getFileName();
        } else if (definition.getVersion().isPresent()) {
            var version = definition.getVersion().get();
            var parsed = SharedUtil.moduleOf(artifact).withVersion(version);
            artifact = parsed.toString();
            downloadUrl = parsed.getDownloadUrl(this.mavenUrl);
            fileName = parsed.getFileName();
        }
        return new Overrides(downloadUrl, fileName, artifact);
    }

    @Override
    public Tool.Resolved get(Provider<? extends Directory> cachesDir, ProviderFactory providers, ToolsExtensionImpl toolsExt) {
        var definition = toolsExt.definitions.maybeCreate(this.getName());

        FileCollection classpathFromGradle = toolsExt.getObjects().fileCollection();
        var classpathFromDownload = definition.getClasspath();
        var mainClass = definition.getMainClass().orElse(providers.provider(this::getMainClass)).getOrNull();

        if (classpathFromDownload.isEmpty()) {
            var overrides = fillOverrides(definition);
            classpathFromGradle = toolsExt.getProject().getConfigurations().detachedConfiguration(
                toolsExt.getDependencies().create(overrides.artifact)
            ).setTransitive(mainClass != null);
            classpathFromDownload = toolsExt.getObjects().fileCollection().from(
                providers.of(Source.class, spec -> spec.parameters(parameters -> {
                    parameters.getInputFile().set(cachesDir.map(d -> d.file("tools/" + overrides.fileName)));
                    parameters.getDownloadUrl().set(overrides.downloadUrl);
                }))
            );
        }

        return new ResolvedImpl(
            toolsExt.getObjects(),
            classpathFromGradle,
            classpathFromDownload,
            mainClass,
            definition.getJavaLauncher().orElse(providers.provider(() -> SharedUtil.launcherForStrictly(toolsExt.getJavaToolchains(), this.getJavaVersion()).get()))
        );
    }

    static abstract class DefinitionImpl implements Definition {
        private final String name;
        private final ConfigurableFileCollection classpath = this.getObjects().fileCollection();
        private final Property<String> mainClass = this.getObjects().property(String.class);
        private final Property<JavaLauncher> javaLauncher = this.getObjects().property(JavaLauncher.class);
        private final Property<String> version = this.getObjects().property(String.class);
        private final Property<String> artifact = this.getObjects().property(String.class);

        protected abstract @Inject ObjectFactory getObjects();

        @Inject
        public DefinitionImpl(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public ConfigurableFileCollection getClasspath() {
            return this.classpath;
        }

        @Override
        public Property<String> getMainClass() {
            return this.mainClass;
        }

        @Override
        public Property<JavaLauncher> getJavaLauncher() {
            return this.javaLauncher;
        }

        @Override
        public Property<String> getVersion() {
            return this.version;
        }

        @Override
        public Property<String> getArtifact() {
            return this.artifact;
        }
    }

    @SuppressWarnings("serial")
    final class ResolvedImpl implements ToolInternal.Resolved {
        private final FileCollection classpathFromGradle;
        private final FileCollection classpathFromDownload;
        private final @Nullable String mainClass;
        private final Property<JavaLauncher> javaLauncher;

        private @Nullable Boolean useGradle = null;

        private ResolvedImpl(ObjectFactory objects, FileCollection classpathFromGradle, FileCollection classpathFromDownload, @Nullable String mainClass, Provider<? extends JavaLauncher> javaLauncher) {
            this.classpathFromGradle = classpathFromGradle;
            this.classpathFromDownload = classpathFromDownload;
            this.mainClass = mainClass;
            this.javaLauncher = objects.property(JavaLauncher.class).value(javaLauncher);
        }

        @Override
        public FileCollection getClasspath() {
            if (useGradle == null) {
                try {
                    useGradle = !classpathFromGradle.getFiles().isEmpty();
                } catch (Exception e) {
                    useGradle = false;
                }
            }

            return useGradle ? classpathFromGradle : classpathFromDownload;
        }

        @Override
        public String getName() {
            return ToolImpl.this.getName();
        }

        @Override
        public ModuleVersionIdentifier getModule() {
            return ToolImpl.this.getModule();
        }

        @Override
        public Property<JavaLauncher> getJavaLauncher() {
            return this.javaLauncher;
        }

        @Override
        public int getJavaVersion() {
            return this.javaLauncher.get().getMetadata().getLanguageVersion().asInt();
        }

        @Override
        public @Nullable String getMainClass() {
            return this.mainClass;
        }
    }

    static abstract class Source implements ValueSource<File, Source.Parameters> {
        interface Parameters extends ValueSourceParameters {
            RegularFileProperty getInputFile();

            Property<String> getDownloadUrl();
        }

        @Inject
        public Source() { }

        @Override
        public File obtain() {
            var parameters = this.getParameters();

            // inputs
            var downloadUrl = parameters.getDownloadUrl().get();

            // outputs
            var outFile = parameters.getInputFile().get().getAsFile();
            var name = outFile.getName();

            // in-house caching
            var cache = HashStore.fromFile(outFile).add("url", downloadUrl);

            if (outFile.exists() && cache.isSame()) {
                LOGGER.info("Default tool already downloaded: {}", name);
            } else {
                try {
                    LOGGER.info("Deleting out-of-date tool: {}", name);
                    Files.deleteIfExists(outFile.toPath());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to delete out-of-date tool: " + name);
                }

                LOGGER.info("Downloading default tool: {}", name);
                try {
                    DownloadUtils.downloadFile(outFile, downloadUrl);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to download default tool: " + name, e);
                }

                cache.save();
            }

            return outFile;
        }
    }
}
