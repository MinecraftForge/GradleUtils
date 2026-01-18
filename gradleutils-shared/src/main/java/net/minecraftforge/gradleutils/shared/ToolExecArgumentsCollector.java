/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.FileSystemLocationProperty;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.UnknownNullability;

import java.io.File;
import java.util.Map;

public interface ToolExecArgumentsCollector {
    void args(Object... args);

    void args(Iterable<?> args);

    void args(String arg, Iterable<? extends File> files);

    void args(String arg, FileSystemLocationProperty<? extends FileSystemLocation> fileProvider);

    void args(String arg, @UnknownNullability Provider<?> provider);

    void args(Map<?, ?> args);

    void jvmArgs(Object... args);

    void jvmArgs(Iterable<?> args);

    void environment(String key, String value);

    void systemProperty(String key, String value);
}
