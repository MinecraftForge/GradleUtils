/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import org.gradle.api.Action;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;

non-sealed interface GradleUtilsExtensionInternal extends GradleUtilsExtension, HasPublicType {
    @Override
    default TypeOf<?> getPublicType() {
        return TypeOf.typeOf(GradleUtilsExtension.class);
    }

    Action<MavenArtifactRepository> forgeMaven = repo -> {
        repo.setName("MinecraftForge");
        repo.setUrl(Constants.FORGE_MAVEN);
    };

    Action<MavenArtifactRepository> forgeReleaseMaven = repo -> {
        repo.setName("MinecraftForge releases");
        repo.setUrl(Constants.FORGE_MAVEN_RELEASE);
    };

    Action<MavenArtifactRepository> minecraftLibsMaven = repo -> {
        repo.setName("Minecraft libraries");
        repo.setUrl(Constants.MC_LIBS_MAVEN);
    };

    /// Unpacks a deferred value or returns `null` if the value could not be unpacked or queried.
    ///
    /// @param value The value to unpack
    /// @param <T>   The type of value held by the provider
    /// @return The unpacked value
    /// @see #unpack(Object)
    default <T> T unpackOrNull(@UnknownNullability Object value) {
        try {
            return this.unpack(Objects.requireNonNull(value));
        } catch (Throwable e) {
            return null;
        }
    }

    non-sealed interface ForProject extends GradleUtilsExtensionInternal, GradleUtilsExtensionForProject, HasPublicType {
        @Override
        default TypeOf<?> getPublicType() {
            return TypeOf.typeOf(GradleUtilsExtensionForProject.class);
        }
    }
}
