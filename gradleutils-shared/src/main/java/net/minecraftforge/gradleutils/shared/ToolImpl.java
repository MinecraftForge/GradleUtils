/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import net.minecraftforge.util.download.DownloadUtils;
import net.minecraftforge.util.hash.HashStore;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

record ToolImpl(String getName, String version, String fileName, String downloadUrl, int getJavaVersion, @Nullable String getMainClass) implements Tool {
    private static final Logger LOGGER = Logging.getLogger(Tool.class);

    ToolImpl(String name, String version, String downloadUrl, int javaVersion, @Nullable String mainClass) {
        this(name, version, "%s-%s.jar".formatted(name, version), downloadUrl, javaVersion, mainClass);
    }

    @Override
    public Provider<File> get(Provider<? extends Directory> cachesDir, ProviderFactory providers) {
        return providers.of(Source.class, spec -> spec.parameters(parameters -> {
            parameters.getInputFile().set(cachesDir.map(d -> d.file("tools/" + this.fileName)));
            parameters.getDownloadUrl().set(this.downloadUrl);
        }));
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
