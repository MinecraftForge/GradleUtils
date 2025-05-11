/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.gitversion

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import net.minecraftforge.gitver.api.GitVersion
import net.minecraftforge.gitver.api.GitVersionException
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nullable

import javax.inject.Inject

/**
 * The heart of the Git Version Gradle plugin. This extension is responsible for creating the GitVersion object and
 * allowing access to it from Gradle buildscripts.
 * <p>When using Gradle's Configuration Cache, the system Git config is disabled.</p>
 */
@CompileStatic
@SuppressWarnings('GrDeprecatedAPIUsage')
class GitVersionExtension {
    public static final String NAME = 'gitversion'

    private final Project project
    private final ObjectFactory objects
    private final ProjectLayout layout
    private final ProviderFactory providers

    /** @deprecated This constructor will be made package-private in GradleUtils 3.0 */
    @Inject
    @Deprecated(forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    GitVersionExtension(Project project, ObjectFactory objects, ProjectLayout layout, ProviderFactory providers) {
        this.project = project
        this.objects = objects
        this.layout = layout
        this.providers = providers
    }


    /* GIT VERSION */

    @Deprecated(forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = '3.0')
    @PackageScope @Lazy GitVersion versionInternal = {
        GitVersion.disableSystemConfig()

        var projectDir = this.layout.projectDirectory.asFile
        var builder = GitVersion.builder().project(projectDir)
        var gitversion = this.tryBuild(builder)
        return gitversion ?: tryBuild(builder.project(GitVersion.findGitRoot(projectDir)))

        /*
        try {
            return builder.build().tap { it.info }
        } catch (GitVersionException ignored) {
            this.project.logger.warn 'WARNING: Git Version failed to get version numbers! Attempting to use default version 0.0.0. Check your GitVersion config file and make sure the correct tag prefix and filters are in use. Ensure that the tags you are attempting to use exist in the repository.'
            return builder.strict(false).build()
        } catch (IllegalArgumentException e) {
            this.project.logger.error 'ERROR: Git Version is misconfigured and cannot be used, likely due to incorrect paths being set. This is an unrecoverable problem and needs to be addressed in the config file. Ensure that the correct subprojects and paths are declared in the config file'
            throw e
        }
         */
    }()

    // TODO [GitVersion][Gradle] Handle this better in GU3.0
    private static @Nullable GitVersion tryBuild(GitVersion.Builder builder) {
        try {
            builder.build().tap { info }
        } catch (GitVersionException ignored) {
            builder.strict(false).build()
        } catch (IllegalArgumentException ignored) {
            null
        }
    }

    // TODO [GradleUtils][3.0] Make private
    private static boolean deprecationWarning
    @Deprecated(forRemoval = true)
    GitVersion getVersion() {
        if (!deprecationWarning) {
            this.project.logger.warn "WARNING: The usage of 'gitversion.version' has been deprecated and will be removed in GradleUtils 3.0. Please remove the 'version' call (i.e. 'gitversion.version.tagOffset' -> 'gitversion.tagOffset')."
            deprecationWarning = true
        }

        this.versionInternal
    }


    /* VERSION NUMBER */

    String getTagOffset() {
        this.versionInternal.tagOffset
    }

    String getTagOffsetBranch() {
        this.versionInternal.tagOffsetBranch
    }

    String getTagOffsetBranch(String... allowedBranches) {
        this.versionInternal.getTagOffsetBranch allowedBranches
    }

    String getTagOffsetBranch(Collection<String> allowedBranches) {
        this.versionInternal.getTagOffsetBranch allowedBranches
    }

    String getMCTagOffsetBranch(String mcVersion) {
        this.versionInternal.getMCTagOffsetBranch mcVersion
    }

    String getMCTagOffsetBranch(String mcVersion, String... allowedBranches) {
        this.versionInternal.getMCTagOffsetBranch mcVersion, allowedBranches
    }

    String getMCTagOffsetBranch(String mcVersion, Collection<String> allowedBranches) {
        this.versionInternal.getMCTagOffsetBranch mcVersion, allowedBranches
    }


    /* INFO */

    GitVersion.Info getInfo() {
        this.versionInternal.info
    }

    @Nullable String getUrl() {
        this.versionInternal.url
    }


    /* FILE SYSTEM */

    @Lazy DirectoryProperty gitDir = {
        this.objects.directoryProperty().fileProvider(this.providers.provider {
            this.versionInternal.gitDir
        })
    }()

    @Lazy DirectoryProperty rootDir = {
        this.objects.directoryProperty().fileProvider(this.providers.provider {
            this.versionInternal.root
        })
    }()

    @Lazy DirectoryProperty projectDir = {
        this.objects.directoryProperty().fileProvider(this.providers.provider {
            this.versionInternal.project
        })
    }()

    @Lazy Property<String> projectPath = {
        this.objects.property(String).value(this.providers.provider {
            this.versionInternal.projectPath
        })
    }()

    Provider<String> getRelativePath(FileSystemLocation file) {
        this.getRelativePath this.providers.provider { file }
    }

    Provider<String> getRelativePath(Provider<? extends FileSystemLocation> file) {
        this.providers.provider {
            this.versionInternal.getRelativePath file.get().asFile
        }
    }


    /* SUBPROJECTS */

    @Lazy ListProperty<Directory> subprojects = {
        this.objects.listProperty(Directory).value(this.providers.provider {
            this.versionInternal.subprojects.collect {
                dir -> this.layout.dir(this.providers.provider { dir }).get()
            }
        })
    }()

    private @Lazy ListProperty<String> subprojectPathsFromRoot = {
        this.objects.listProperty(String).value(this.providers.provider {
            this.versionInternal.getSubprojectPaths true
        })
    }()

    @Lazy ListProperty<String> subprojectPaths = {
        this.objects.listProperty(String).value(this.providers.provider {
            this.versionInternal.subprojectPaths
        })
    }()

    ListProperty<String> getSubprojectPaths(boolean fromRoot) {
        fromRoot ? this.subprojectPathsFromRoot : this.subprojectPaths
    }
}
