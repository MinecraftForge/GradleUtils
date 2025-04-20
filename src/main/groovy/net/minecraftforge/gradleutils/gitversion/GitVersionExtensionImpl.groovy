/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.gitversion

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import net.minecraftforge.gitver.api.GitVersion
import net.minecraftforge.gitver.api.GitVersionException
import org.gradle.api.configuration.BuildFeatures
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.annotations.Nullable

@CompileStatic
@PackageScope([PackageScopeTarget.CLASS, PackageScopeTarget.CONSTRUCTORS])
class GitVersionExtensionImpl implements GitVersionExtension {
    private static final Logger LOGGER = Logging.getLogger GitVersionExtension

    private final Directory projectDirectory
    private final ObjectFactory objects
    private final ProviderFactory providers
    private final BuildFeatures buildFeatures

    GitVersionExtensionImpl(Directory projectDirectory, ObjectFactory objects, ProviderFactory providers, BuildFeatures buildFeatures) {
        this.projectDirectory = projectDirectory
        this.objects = objects
        this.providers = providers
        this.buildFeatures = buildFeatures
    }


    /* GIT VERSION */

    private @Lazy GitVersion version = {
        // If we are using the configuration cache, disable the system config since it calls the git command line tool
        if (this.buildFeatures.configurationCache.active.getOrElse(false))
            GitVersion.disableSystemConfig()

        var builder = GitVersion.builder().project this.projectDirectory.asFile
        try {
            return builder.build().tap { it.info }
        } catch (GitVersionException ignored) {
            LOGGER.warn 'WARNING: Git Version failed to get version numbers! Attempting to use default version 0.0.0. Check your GitVersion config file and make sure the correct tag prefix and filters are in use. Ensure that the tags you are attempting to use exist in the repository.'
            return builder.strict(false).build()
        } catch (IllegalArgumentException e) {
            LOGGER.error 'ERROR: Git Version is misconfigured and cannot be used, likely due to incorrect paths being set. This is an unrecoverable problem and needs to be addressed in the config file. Ensure that the correct subprojects and paths are declared in the config file'
            throw e
        }
    }()


    /* VERSION NUMBER */

    String getTagOffset() {
        this.version.tagOffset
    }

    String getTagOffsetBranch() {
        this.version.tagOffsetBranch
    }

    String getTagOffsetBranch(String... allowedBranches) {
        this.version.getTagOffsetBranch allowedBranches
    }

    String getTagOffsetBranch(Collection<String> allowedBranches) {
        this.version.getTagOffsetBranch allowedBranches
    }

    String getMCTagOffsetBranch(String mcVersion) {
        this.version.getMCTagOffsetBranch mcVersion
    }

    String getMCTagOffsetBranch(String mcVersion, String... allowedBranches) {
        this.version.getMCTagOffsetBranch mcVersion, allowedBranches
    }

    String getMCTagOffsetBranch(String mcVersion, Collection<String> allowedBranches) {
        this.version.getMCTagOffsetBranch mcVersion, allowedBranches
    }


    /* INFO */

    GitVersion.Info getInfo() {
        this.version.info
    }

    @Nullable String getUrl() {
        this.version.url
    }


    /* FILE SYSTEM */

    @Lazy DirectoryProperty gitDir = {
        this.objects.directoryProperty().fileProvider(this.providers.provider {
            this.version.gitDir
        })
    }()

    @Lazy DirectoryProperty rootDir = {
        this.objects.directoryProperty().fileProvider(this.providers.provider {
            this.version.root
        })
    }()

    @Lazy DirectoryProperty projectDir = {
        this.objects.directoryProperty().fileProvider(this.providers.provider {
            this.version.project
        })
    }()

    @Lazy Property<String> projectPath = {
        this.objects.property(String).value(this.providers.provider {
            this.version.projectPath
        })
    }()

    Provider<String> getRelativePath(FileSystemLocation file) {
        this.getRelativePath this.providers.provider { file }
    }

    Provider<String> getRelativePath(Provider<? extends FileSystemLocation> file) {
        this.providers.provider {
            this.version.getRelativePath file.get().asFile
        }
    }


    /* SUBPROJECTS */

    @Lazy List<DirectoryProperty> subprojects = {
        this.version.subprojects.collect {
            dir -> this.objects.directoryProperty().fileProvider(this.providers.provider { dir })
        }
    }()

    private @Lazy ListProperty<String> subprojectPathsFromRoot = {
        this.objects.listProperty(String).value(this.providers.provider {
            this.version.getSubprojectPaths true
        })
    }()

    @Lazy ListProperty<String> subprojectPaths = {
        this.objects.listProperty(String).value(this.providers.provider {
            this.version.subprojectPaths
        })
    }()

    ListProperty<String> getSubprojectPaths(boolean fromRoot) {
        fromRoot ? this.subprojectPathsFromRoot : this.subprojectPaths
    }
}
