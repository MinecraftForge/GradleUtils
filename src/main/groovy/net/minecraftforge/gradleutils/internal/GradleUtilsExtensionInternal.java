/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.internal;

import net.minecraftforge.gradleutils.GradleUtilsExtension;
import net.minecraftforge.gradleutils.GradleUtilsExtensionForProject;
import org.gradle.api.Action;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.jetbrains.annotations.UnknownNullability;

import java.io.File;
import java.util.Objects;

interface GradleUtilsExtensionInternal extends GradleUtilsExtension, HasPublicType {
    @Override
    default TypeOf<?> getPublicType() {
        return TypeOf.typeOf(GradleUtilsExtension.class);
    }

    // NOTE: Rename from forgeMaven -> FORGE_MAVEN in GU4
    Action<MavenArtifactRepository> forgeMaven = repo -> {
        repo.setName("MinecraftForge");
        repo.setUrl(Constants.FORGE_MAVEN);
    };

    // NOTE: Rename from forgeReleaseMaven -> FORGE_MAVEN_RELEASES in GU4
    Action<MavenArtifactRepository> forgeReleaseMaven = repo -> {
        repo.setName("MinecraftForge releases");
        repo.setUrl(Constants.FORGE_MAVEN_RELEASE);
    };

    // NOTE: Rename from minecraftLibsMaven -> MINECRAFT_LIBS_MAVEN in GU4
    Action<MavenArtifactRepository> minecraftLibsMaven = repo -> {
        repo.setName("Minecraft libraries");
        repo.setUrl("https://libraries.minecraft.net/");
    };

    @Override
    default Action<MavenArtifactRepository> getForgeMaven() {
        return forgeMaven;
    }

    @Override
    default Action<MavenArtifactRepository> getForgeReleaseMaven() {
        return forgeReleaseMaven;
    }

    @Override
    default Action<MavenArtifactRepository> getMinecraftLibsMaven() {
        return minecraftLibsMaven;
    }

    @Override
    default Action<MavenArtifactRepository> getPublishingForgeMaven() {
        return getPublishingForgeMaven(Constants.FORGE_MAVEN_RELEASE);
    }

    @Override
    default Action<MavenArtifactRepository> getPublishingForgeMaven(File defaultFolder) {
        return this.getPublishingForgeMaven(Constants.FORGE_MAVEN_RELEASE, defaultFolder);
    }

    @Override
    default Action<MavenArtifactRepository> getPublishingForgeMaven(Directory defaultFolder) {
        return this.getPublishingForgeMaven(Constants.FORGE_MAVEN_RELEASE, defaultFolder);
    }

    @Override
    default Action<MavenArtifactRepository> getPublishingForgeMaven(Provider<?> defaultFolder) {
        return this.getPublishingForgeMaven(Constants.FORGE_MAVEN_RELEASE, defaultFolder);
    }

    @Override
    default <T> T unpack(Object value) {
        return Util.unpack(value);
    }

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

    interface ForProject extends GradleUtilsExtensionInternal, GradleUtilsExtensionForProject, HasPublicType {
        @Override
        default TypeOf<?> getPublicType() {
            return TypeOf.typeOf(GradleUtilsExtensionForProject.class);
        }
    }
}
