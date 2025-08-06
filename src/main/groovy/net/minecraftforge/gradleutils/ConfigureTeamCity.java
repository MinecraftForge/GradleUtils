/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;

// TODO [GradleUtils][TeamCity] Delete this when off of TeamCity
@Deprecated(forRemoval = true)
abstract class ConfigureTeamCity extends DefaultTask implements HasPublicType {
    static final String NAME = "configureTeamCity";

    @Override
    public TypeOf<?> getPublicType() {
        // We don't want this task to be configurable, so tell Gradle it's just a default task
        return TypeOf.typeOf(DefaultTask.class);
    }

    @Inject
    public ConfigureTeamCity(ProviderFactory providers) {
        this.setGroup("Build Setup");
        this.setDescription("Prints the marker lines into the log which configure the pipeline. [deprecated]");
        this.onlyIf("Only runs on TeamCity, so the TEAMCITY_VERSION environment variable must be set.", task -> providers.environmentVariable("TEAMCITY_VERSION").isPresent());

        this.getBuildNumber().convention(providers.provider(() -> this.getProject().getVersion()).map(Object::toString));
    }

    /** The build number to print, usually the project version. */
    protected abstract @Input Property<String> getBuildNumber();

    @TaskAction
    public void exec() {
        this.getLogger().warn("WARNING: Usage of TeamCity is deprecated within Minecraft Forge Minecraft Forge has been gradually moving off of TeamCity and into GitHub Actions. When the migration is fully complete, this task along with its automatic setup will be removed.");

        final var buildNumber = this.getBuildNumber().get();
        this.getLogger().lifecycle("Setting project variables and parameters.");
        System.out.printf("##teamcity[buildNumber '%s']%n", buildNumber);
        System.out.printf("##teamcity[setParameter name='env.PUBLISHED_JAVA_ARTIFACT_VERSION' value='%s']%n", buildNumber);
    }
}
