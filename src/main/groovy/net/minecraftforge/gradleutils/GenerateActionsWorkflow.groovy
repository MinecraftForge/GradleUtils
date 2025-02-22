package net.minecraftforge.gradleutils

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

abstract class GenerateActionsWorkflow extends DefaultTask {
    private static final String DEFAULT_NAME = 'generateActionsWorkflow'

    static TaskProvider<GenerateActionsWorkflow> register(Project project) {
        return register(project, DEFAULT_NAME)
    }

    static TaskProvider<GenerateActionsWorkflow> register(Project project, String name) {
        var task = project.tasks.register(name, GenerateActionsWorkflow)

        // configure artifact
//        project.configurations.register(name) { canBeResolved = false }
//        project.artifacts.add(name, task)

        project.plugins.withType(LifecycleBasePlugin) {
            project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).configure {
                dependsOn(task)
            }
        }

        return task
    }

    GenerateActionsWorkflow() {
        this.projectPath.convention this.project.provider { GradleUtils.makeFilterFromSubproject(this.project) }
        this.subprojectFilters.convention this.project.provider { this.project.subprojects.collect { "!${GradleUtils.makeFilterFromSubproject(it)}/**".toString() } }
        this.tagPrefix.convention this.project.provider { this.project.extensions.getByType(GradleUtilsExtension).gitInfo.tagPrefix }
    }

    @Input
    @Optional
    abstract Property<String> getProjectPath();

    @Input
    @Optional
    abstract ListProperty<String> getSubprojectFilters();

    @Input
    @Optional
    abstract Property<String> getTagPrefix();

    @TaskAction
    void exec() throws IOException {
        def push = ['branches': 'master'] as Map

        var path = this.projectPath.getOrNull()
        var ignoredPaths = this.subprojectFilters.getOrElse(Collections.emptyList())
        var tagPrefix = this.tagPrefix.getOrNull()
        
        push.put('paths', new ArrayList<String>().tap {
            if (path) it.add(path + '/**')

            it.add('!settings.gradle')
            it.addAll(ignoredPaths)
        })

        def with = [
            'java': 21,
            'gradle_tasks': "${path ? ":${path}:" : ''}build".toString()
        ] as Map
        if (path) with.put('subproject', path)
        if (tagPrefix) with.put('tag_prefix', this.tagPrefix.get())

        Map yaml = [
            'name': 'Build',
            'on': ['push': push],
            'permissions': ['contents': 'read'],
            'jobs': [
                'build': [
                    'uses': 'MinecraftForge/SharedActions/.github/workflows/gradle.yml@main',
                    'with': with,
                    'secrets': [
                        'DISCORD_WEBHOOK': '${{ secrets.DISCORD_WEBHOOK }}'
                    ]
                ]
            ]
        ]
        var workflow = new Yaml(
            new DumperOptions().tap {
                explicitStart = false
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                prettyFlow = true
            }
        ).dump(yaml).replace("'on':", 'on:')

        logger.lifecycle(workflow)
    }
}
