/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.java.archives.internal.ManifestInternal;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.jetbrains.annotations.ApiStatus;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;

@ApiStatus.Internal
@ApiStatus.Experimental
public abstract class WriteManifest extends DefaultTask {
    protected abstract @Input Property<byte[]> getInputBytes();
    protected abstract @OutputFile RegularFileProperty getOutput();

    protected abstract @Inject ProjectLayout getLayout();

    @Inject
    public WriteManifest(TaskProvider<? extends Jar> jar) {
        // The output name is ALWAYS "MANIFEST.MF", and output cannot be changed
        this.getOutput().value(getLayout().getBuildDirectory().file(this.getName() + "/MANIFEST.MF")).disallowChanges();

        var tasks = getProject().getTasks();
        var sourceSet = findSourceSet(jar.getName());

        tasks.named(sourceSet.getProcessResourcesTaskName(), ProcessResources.class, processResources -> {
            processResources.dependsOn(this);
            processResources.from(this, copy -> {
                copy.into("META-INF");
                copy.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE);
            });
        });

        Action<? super Project> afterEvaluate = project -> {
            try (var os = new ByteArrayOutputStream()) {
                // RATIONALE: ManifestInternal has not changed since Gradle 2.14
                // Due to the hacky nature of needing the proper manifest in the resources, this is the only good way of doing this
                // The DefaultManifest object cannot be serialized into the Gradle cache, and the normal Manifest interface does not have this method
                // This should be the only Gradle internals we need to use in all of ForgeDev, thankfully
                ((ManifestInternal) jar.get().getManifest()).writeTo(os);
                this.getInputBytes().value(os.toByteArray()).finalizeValue();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        try {
            getProject().afterEvaluate(afterEvaluate);
        } catch (Exception ignored) {
            afterEvaluate.execute(getProject());
        }
    }

    @TaskAction
    protected void exec() throws IOException {
        Files.write(
            this.getOutput().getAsFile().get().toPath(),
            this.getInputBytes().get()
        );
    }

    private SourceSet findSourceSet(String jarTaskName) {
        var java = getProject().getExtensions().getByType(JavaPluginExtension.class);
        var candidates = java.getSourceSets().matching(sourceSet -> sourceSet.getJarTaskName().equals(jarTaskName)).iterator();
        return candidates.hasNext() ? candidates.next() : java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    }
}
