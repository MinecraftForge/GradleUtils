package net.minecraftforge.gradleutils.gitversion;

import net.minecraftforge.gitver.api.GitVersion;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * The extension for the Git Version plugin. This is the main interface for interacting with Git Version in your script.
 * <p>By design, this is a mirror of the {@link net.minecraftforge.gitver.api.GitVersion} interface. The actual Git
 * Version object is hidden from public consumption, and is delegated to by this interface.</p>
 */
public interface GitVersionExtension {
    String NAME = "gitversion";


    /* VERSION NUMBER */

    String getTagOffset();

    String getTagOffsetBranch();

    String getTagOffsetBranch(String... allowedBranches);

    String getTagOffsetBranch(Collection<String> allowedBranches);

    String getMCTagOffsetBranch(String mcVersion);

    String getMCTagOffsetBranch(String mcVersion, String... allowedBranches);

    String getMCTagOffsetBranch(String mcVersion, Collection<String> allowedBranches);


    /* INFO */

    GitVersion.Info getInfo();

    @Nullable String getUrl();


    /* FILE SYSTEM */

    DirectoryProperty getGitDir();

    DirectoryProperty getRootDir();

    DirectoryProperty getProjectDir();

    Property<String> getProjectPath();

    Provider<String> getRelativePath(FileSystemLocation file);

    Provider<String> getRelativePath(Provider<? extends FileSystemLocation> file);


    /* SUBPROJECTS */

    List<DirectoryProperty> getSubprojects();

    ListProperty<String> getSubprojectPaths();

    ListProperty<String> getSubprojectPaths(boolean fromRoot);
}
