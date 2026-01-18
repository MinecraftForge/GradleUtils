/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.FileSystemLocationProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderConvertible;
import org.gradle.api.provider.ProviderFactory;
import org.jetbrains.annotations.UnknownNullability;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

abstract class ToolExecArgumentsCollectorImpl implements ToolExecArgumentsCollector {
    final List<Provider<String>> args = new ArrayList<>();
    final List<Provider<String>> jvmArgs = new ArrayList<>();
    final Map<String, String> environment = new HashMap<>();
    final Map<String, String> systemProperties = new HashMap<>();

    protected abstract @Inject ProviderFactory getProviders();

    @Inject
    public ToolExecArgumentsCollectorImpl() { }

    @Override
    public void args(Object... args) {
        this.args(Arrays.asList(args));
    }

    @Override
    public void args(Iterable<?> args) {
        try {
            for (var arg : args) {
                if (arg instanceof ProviderConvertible<?> providerConvertible)
                    this.args.add(providerConvertible.asProvider().map(Object::toString));
                if (arg instanceof Provider<?> provider)
                    this.args.add(provider.map(Object::toString));
                else
                    this.args.add(this.getProviders().provider(arg::toString));
            }
        } catch (NullPointerException e) {
            throw new IllegalStateException("ToolExecBase#jvmArgs can only be called inside of #addArguments()", e);
        }
    }

    /// Adds each file to the arguments preceded by the given argument. Designed to work well with
    /// <a href="https://jopt-simple.github.io/jopt-simple/">JOpt Simple</a>.
    ///
    /// @param arg   The flag to use for each file
    /// @param files The files to add
    @Override
    public void args(String arg, Iterable<? extends File> files) {
        for (File file : files)
            this.args(arg, file);
    }

    /// Adds the given argument followed by the given file location to the arguments.
    ///
    /// @param arg          The flag to use
    /// @param fileProvider The file to add
    @Override
    public void args(String arg, FileSystemLocationProperty<? extends FileSystemLocation> fileProvider) {
        this.args(arg, fileProvider.getLocationOnly());
    }

    /// Adds the given argument followed by the given object (may be a file location) to the arguments.
    ///
    /// @param arg      The flag to use
    /// @param provider The object (or file) to add
    @Override
    public void args(String arg, @UnknownNullability Provider<?> provider) {
        if (provider == null || !provider.isPresent()) return;

        // NOTE: We don't use File#getAbsoluteFile because path sensitivity should be handled by tasks.
        var value = provider.map(it -> it instanceof FileSystemLocation ? ((FileSystemLocation) it).getAsFile() : it).getOrNull();
        if (value == null) return;

        if (value instanceof Boolean booleanValue) {
            if (booleanValue)
                this.args(arg);
        } else {
            this.args(arg, String.valueOf(value));
        }
    }

    /// Adds the given map of arguments.
    ///
    /// [#args(String, Provider)] will be invoked for each entry in the map. If the key and/or value are not of the
    /// required types, they will be automatically converted using [Object#toString()] and
    /// [org.gradle.api.provider.ProviderFactory#provider(Callable)].
    ///
    /// @param args The args to add
    /// @deprecated Too ambiguous with [#args(String, Provider)]. Prefer that method instead.
    @Override
    @Deprecated(forRemoval = true)
    public void args(Map<?, ?> args) {
        for (Map.Entry<?, ?> entry : args.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            this.args(
                key instanceof Provider<?> provider ? provider.map(Object::toString).get() : this.getProviders().provider(key::toString).get(),
                value instanceof Provider<?> provider ? (provider instanceof FileSystemLocationProperty<?> file ? file.getLocationOnly() : provider) : this.getProviders().provider(() -> value)
            );
        }
    }

    @Override
    public void jvmArgs(Object... args) {
        try {
            for (var arg : args) {
                this.jvmArgs.add(this.getProviders().provider(arg::toString));
            }
        } catch (NullPointerException e) {
            throw new IllegalStateException("ToolExecBase#jvmArgs can only be called inside of #addArguments()", e);
        }
    }

    @Override
    public void jvmArgs(Iterable<?> args) {
        try {
            for (var arg : args) {
                this.jvmArgs.add(this.getProviders().provider(arg::toString));
            }
        } catch (NullPointerException e) {
            throw new IllegalStateException("ToolExecBase#jvmArgs can only be called inside of #addArguments()", e);
        }
    }

    @Override
    public void environment(String key, String value) {
        try {
            this.environment.put(key, value);
        } catch (NullPointerException e) {
            throw new IllegalStateException("ToolExecBase#environment can only be called inside of #addArguments()", e);
        }
    }

    @Override
    public void systemProperty(String key, String value) {
        try {
            this.systemProperties.put(key, value);
        } catch (NullPointerException e) {
            throw new IllegalStateException("ToolExecBase#systemProperty can only be called inside of #addArguments()", e);
        }
    }
}
