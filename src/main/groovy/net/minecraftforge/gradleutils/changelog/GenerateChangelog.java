package net.minecraftforge.gradleutils.changelog;

import net.minecraftforge.gitver.api.GitVersion;
import net.minecraftforge.gitver.api.GitVersionException;
import org.gradle.api.DefaultTask;
import org.gradle.api.configuration.BuildFeatures;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Generates a changelog for the project based on the Git history using
 * <a href="https://github.com/MinecraftForge/GitVersion">Git Version</a>.
 */
public abstract class GenerateChangelog extends DefaultTask {
    /** The name for the task, used by the {@linkplain ChangelogExtension extension} when registering it. */
    public static final String NAME = "createChangelog";

    private final BuildFeatures buildFeatures;

    /**
     * Constructs a new task instance.
     * <p>This constructor is invoked by Gradle when used with
     * {@link org.gradle.api.tasks.TaskContainer#register(String, Class)} to {@linkplain Inject inject} the required
     * services used by this taks.</p>
     *
     * @param objects       The object factory used to create properties
     * @param layout        The project layout used to resolve project path from the Git root for Git Version
     * @param providers     The provider factory used to assist in creating properties
     * @param buildFeatures The build features for configuration cache awareness
     */
    @Inject
    GenerateChangelog(ObjectFactory objects, ProjectLayout layout, ProviderFactory providers, BuildFeatures buildFeatures) {
        this.buildFeatures = buildFeatures;

        this.setDescription("Generates a changelog for the project based on the Git history using Git Version.");

        //Setup defaults: Using merge-base based text changelog generation of the local project into build/changelog.txt
        this.getOutputFile().convention(layout.getBuildDirectory().file("changelog.txt"));

        this.getGitDirectory().convention(objects.directoryProperty().fileProvider(providers.provider(
            () -> GitVersion.findGitRoot(layout.getProjectDirectory().getAsFile()))
        ).dir(".git"));
        this.getProjectPath().convention(providers.provider(() -> {
            var root = this.getGitDirectory().get().getAsFile().toPath();
            var project = layout.getProjectDirectory().getAsFile().toPath();
            return root.relativize(project).toString().replace(root.getFileSystem().getSeparator(), "/");
        }));
        this.getBuildMarkdown().convention(false);
    }

    /**
     * The output file for the changelog.
     *
     * @return A property for the output file
     */
    public abstract @OutputFile RegularFileProperty getOutputFile();

    /**
     * The {@code .git} directory to base the Git Version off of.
     * <p>Git Version will automatically attempt to locate this using {@link GitVersion#findGitRoot(File)} if left
     * unspecified.</p>
     *
     * @return A property for the Git directory
     */
    public abstract @InputDirectory @PathSensitive(PathSensitivity.ABSOLUTE) DirectoryProperty getGitDirectory();

    /**
     * The path string of the project from the root.
     * <p>Used to configure Git Version without needing to specify the directory itself, since using the directory
     * itself can cause implicit dependencies on other tasks that use actually it.</p>
     *
     * @return A property for the project path
     */
    public abstract @Input Property<String> getProjectPath();

    /**
     * The tag (or object ID) to start the changelog from.
     *
     * @return A property for the start tag
     */
    public abstract @Input @Optional Property<String> getStart();

    /**
     * The project URL to use in the changelog.
     * <p>Git Version will automatically attempt to find a URL from the repository's remote details if left
     * unspecified.</p>
     *
     * @return A property for the project URL
     */
    public abstract @Input @Optional Property<String> getProjectUrl();

    /**
     * Whether to build the changelog in Markdown format.
     *
     * @return A property for Markdown formatting
     */
    public abstract @Input Property<Boolean> getBuildMarkdown();

    /** Executes the task to generate the changelog. */
    @TaskAction
    public void exec() {
        // If we are using the configuration cache, disable the system config since it calls the git command line tool
        if (this.buildFeatures.getConfigurationCache().getActive().getOrElse(false))
            GitVersion.disableSystemConfig();

        var gitDir = this.getGitDirectory().getAsFile().get();
        try (var version = GitVersion.builder().gitDir(gitDir).project(new File(gitDir.getAbsoluteFile().getParentFile(), this.getProjectPath().get())).build()) {
            var changelog = version.generateChangelog(this.getStart().getOrNull(), this.getProjectUrl().getOrNull(), !this.getBuildMarkdown().get());

            var file = this.getOutputFile().get().getAsFile();
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs())
                throw new IllegalStateException();

            Files.writeString(
                file.toPath(),
                changelog,
                StandardCharsets.UTF_8
            );
        } catch (GitVersionException e) {
            this.getLogger().error("ERROR: Failed to generate the changelog for this project, likely due to a misconfiguration. GitVersion has caught the exception, the details of which are attached to this error. Check that the correct tags are being used, or updating the tag prefix accordingly.");
            throw e;
        } catch (IOException e) {
            this.getLogger().error("ERROR: Changelog was generated successfully, but could not be written to the disk. Ensure that you have write permissions to the output directory.");
            throw new RuntimeException(e);
        }
    }
}
