/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import net.minecraftforge.util.download.DownloadUtils;
import net.minecraftforge.util.hash.HashStore;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

class ToolImpl implements ToolInternal {
    private static final long serialVersionUID = -862411638019629688L;

    private static final Logger LOGGER = Logging.getLogger(Tool.class);

    private final String name;
    private final String version;
    private final String fileName;
    private final String downloadUrl;
    private final int javaVersion;
    private final @Nullable String mainClass;

    public ToolImpl(String name, String version, String fileName, String downloadUrl, int javaVersion, @Nullable String mainClass) {
        this.name = name;
        this.version = version;
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.javaVersion = javaVersion;
        this.mainClass = mainClass;
    }

    ToolImpl(String name, String version, String downloadUrl, int javaVersion, @Nullable String mainClass) {
        this(name, version, String.format("%s-%s.jar", name, version), downloadUrl, javaVersion, mainClass);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public int getJavaVersion() {
        return this.javaVersion;
    }

    @Override
    public @Nullable String getMainClass() {
        return this.mainClass;
    }

    @Override
    public Tool.Resolved get(Provider<? extends Directory> cachesDir, ToolsExtensionImpl toolsExt) {
        Tool.Definition definition = toolsExt.definitions.maybeCreate(this.name);
        FileCollection classpath = definition.getClasspath();
        if (classpath.isEmpty()) {
            classpath = toolsExt.getObjects().fileCollection().from(
                toolsExt.getProviders().of(Source.class, spec -> spec.parameters(parameters -> {
                    parameters.getInputFile().set(cachesDir.map(d -> d.file("tools/" + this.fileName)));
                    parameters.getDownloadUrl().set(this.downloadUrl);
                }))
            );
        }

        return new ResolvedImpl(
            toolsExt.getObjects(),
            classpath,
            definition.getMainClass().orElse(toolsExt.getProviders().provider(this::getMainClass)),
            definition.getJavaLauncher().orElse(SharedUtil.launcherForStrictly(toolsExt.getJavaToolchains(), this.getJavaVersion()))
        );
    }

    @SuppressWarnings("serial")
    private class ResolvedImpl implements Tool.Resolved {
        private final FileCollection classpath;
        private final Property<String> mainClass;
        private final Property<JavaLauncher> javaLauncher;

        private ResolvedImpl(ObjectFactory objects, FileCollection classpath, Provider<? extends String> mainClass, Provider<? extends JavaLauncher> javaLauncher) {
            this.classpath = classpath;
            this.mainClass = objects.property(String.class).value(mainClass);
            this.javaLauncher = objects.property(JavaLauncher.class).value(javaLauncher);
        }

        @Override
        public FileCollection getClasspath() {
            return this.classpath;
        }

        @Override
        public String getName() {
            return ToolImpl.this.getName();
        }

        @Override
        public String getVersion() {
            return ToolImpl.this.getVersion();
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
            return this.mainClass.getOrNull();
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
            Parameters parameters = this.getParameters();

            // inputs
            String downloadUrl = parameters.getDownloadUrl().get();

            // outputs
            File outFile = parameters.getInputFile().get().getAsFile();
            String name = outFile.getName();

            // in-house caching
            HashStore cache = HashStore.fromFile(outFile).add("url", downloadUrl);

            if (outFile.exists() && cache.isSame()) {
                LOGGER.info("Default tool already downloaded: {}", name);
            } else {
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
