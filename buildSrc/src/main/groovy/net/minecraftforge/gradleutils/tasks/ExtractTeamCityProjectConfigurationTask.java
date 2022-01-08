/*
 * GradleUtils
 * Copyright (C) 2021 Forge Development LLC
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package net.minecraftforge.gradleutils.tasks;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.gradle.api.DefaultTask;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Task to extract the TeamCity project configuration from the included template zpi file and the github remote information.
 */
public abstract class ExtractTeamCityProjectConfigurationTask extends DefaultTask
{

    public ExtractTeamCityProjectConfigurationTask()
    {
        getDestination().convention(getProject().getRootProject().getLayout().getProjectDirectory().dir(getProject().provider(() -> "./")));

        setGroup("publishing");
        setDescription("Creates (or recreates) a default TeamCity project configuration directory for use with the MinecraftForge TeamCity server.");
    }

    @InputDirectory
    @PathSensitive(PathSensitivity.NONE)
    public abstract DirectoryProperty getDestination();

    @TaskAction
    public void run() throws Exception
    {
        //Get the destination directory (by default the current root project directory)
        final File destDir = getDestination().getAsFile().get();
        //Grab the target directory, to check if it exists.
        final File teamcityDir = new File(destDir, ".teamcity");

        //Export the zip file from our resources.
        String fileZip = exportResource();

        //Check if the directory exists, if so then delete it.
        if (teamcityDir.exists())
        {
            //Try to delete it.
            if (!teamcityDir.delete())
            {
                //Something went really wrong....
                throw new IllegalStateException("Could not delete the existing .teamcity project directory!");
            }
        }

        //Extract the zip from our runtime file.
        extractTeamCityZip(fileZip, destDir);
        //Replace the default project ids, with ours.
        replaceTeamCityTestProjectIds(destDir);
    }

    /**
     * Extracts the .teamcity.zip file to our target directory.
     *
     * @param fileZip The teamcity zip file.
     * @param destDir The target directory (generally the project directory), where the .teamcity.zip file will be extracted.
     */
    private static void extractTeamCityZip(final String fileZip, final File destDir) throws Exception
    {
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    /**
     * Extracts the .teamcity.zip file from our plugin jar.
     *
     * @return The path to the file.
     */
    private static String exportResource() throws Exception
    {
        InputStream stream = null;
        OutputStream resStreamOut = null;
        String jarFolder;
        try {
            stream = ExtractTeamCityProjectConfigurationTask.class.getResourceAsStream("/.teamcity.zip");//note that each / is a directory down in the "jar tree" been the jar the root of the tree
            if(stream == null) {
                throw new Exception("Cannot get resource \"" + ".teamcity.zip" + "\" from Jar file.");
            }

            int readBytes;
            byte[] buffer = new byte[4096];
            jarFolder = new File(ExtractTeamCityProjectConfigurationTask.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath().replace('\\', '/');
            resStreamOut = new FileOutputStream(jarFolder + ".teamcity.zip");
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        }
        finally {
            if (stream != null)
            {
                stream.close();
            }
            if (resStreamOut != null)
            {
                resStreamOut.close();
            }
        }

        return jarFolder + ".teamcity.zip";
    }

    /**
     * Creates a new file or directory, used during the extraction of the .teamcity.zip file.
     *
     * @param destinationDir The target directory.
     * @param zipEntry The entry in the .teamcity.zip file.
     * @return The new file or directory.
     */
    private static File newFile(File destinationDir, ZipEntry zipEntry) throws Exception
    {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    /**
     * Parses the existing .teamcity directory files for "TeamCityTest" and replaces it with a new project id,
     * generated by checking the first remote on the current git project.
     *
     * @param projectDir The project directory to run the replacement in.
     */
    @SuppressWarnings("ReadWriteStringCanBeUsed") //We still need to support older versions of the JDK.
    private void replaceTeamCityTestProjectIds(final File projectDir) throws Exception
    {
        final String projectId = determineGitHubProjectName(projectDir);
        final File teamcityDir = new File(projectDir, ".teamcity");
        if (!teamcityDir.exists())
        {
            return;
        }

        for (final File file : Objects.requireNonNull(teamcityDir.listFiles((dir, name) -> name.endsWith("kts"))))
        {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            content = content.replaceAll("%projectName%", projectId);
            content = content.replaceAll("%projectOrg%", determineGitHubProjectOrganisation(projectDir));
            content = content.replaceAll("%projectArtifactId%", determineArtifactId(projectId));
            content = content.replaceAll("%projectArtifactGroup%", determineGroup(getProject().getGroup().toString()));
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        }

        String projectGroup = determineGitHubProjectOrganisation(projectDir);
        if (!projectGroup.equals("MinecraftForge")) {
            projectGroup = "MinecraftForge_" + projectGroup;
        }

        for (final File file : Objects.requireNonNull(teamcityDir.listFiles((dir, name) -> name.endsWith("xml"))))
        {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            content = content.replaceAll("%projectName%", projectId);
            content = content.replaceAll("%projectGroup%", projectGroup);
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String determineArtifactId(String projectId) {
        if (getProject().getExtensions().findByType(PublishingExtension.class) == null) {
            getProject().getLogger().warn("Could not find the Maven publication extension, falling back to the lower cased project name.");
            return projectId.toLowerCase();
        }

        try {
            return getProject().getExtensions().getByType(PublishingExtension.class)
              .getPublications()
              .stream()
              .filter(MavenPublication.class::isInstance)
              .map(MavenPublication.class::cast)
              .filter(publication -> !publication.getName().contains("PluginMarker")) //Exclude gradles plugin markers!
              .findFirst()
              .map(MavenPublication::getArtifactId)
              .orElseGet(() -> {
                  getProject().getLogger().warn("Could not find the Maven artifact Id from normal publication falling back to the lower cased project name.");
                  return projectId.toLowerCase();
              });
        }
        catch (UnknownDomainObjectException unknownDomainObjectException) {
            getProject().getLogger().warn("Could not find the Maven publication extension, falling back to the lower cased project name.");
            return projectId.toLowerCase();
        }

    }

    private String determineGroup(String fallback) {
        if (getProject().getExtensions().findByType(PublishingExtension.class) == null) {
            getProject().getLogger().warn("Could not find the Maven publication extension, falling back to the lower cased project group.");
            return fallback.toLowerCase();
        }

        try {
            return getProject().getExtensions().getByType(PublishingExtension.class)
              .getPublications()
              .stream()
              .filter(MavenPublication.class::isInstance)
              .map(MavenPublication.class::cast)
              .filter(publication -> !publication.getName().contains("PluginMarker")) //Exclude gradles plugin markers!
              .findFirst()
              .map(MavenPublication::getGroupId)
              .orElseGet(() -> {
                  getProject().getLogger().warn("Could not find the Maven artifact Id from normal publication falling back to the lower cased project group.");
                  return fallback.toLowerCase();
              });
        }
        catch (UnknownDomainObjectException unknownDomainObjectException) {
            getProject().getLogger().warn("Could not find the Maven publication extension, falling back to the lower cased project group.");
            return fallback.toLowerCase();
        }
    }

    /**
     * Determines the project name of the project of github.
     * Querries the first remote of the current git project and pulls its fetch URL information to extract the name.
     *
     * @param projectDir The project directory.
     * @return The project name of the project on github.
     */
    private static String determineGitHubProjectName(final File projectDir) throws Exception
    {
        final Git git = Git.open(projectDir);
        final String repositoryPath = git.remoteList().call().get(0).getURIs().get(0).getPath();

        return repositoryPath.substring(repositoryPath.lastIndexOf("/") + 1).replace(".git", "");
    }

    /**
     * Determines the project name of the organisation of github.
     * Querries the first remote of the current git project and pulls its fetch URL information to extract the name.
     *
     * @param projectDir The project directory.
     * @return The organisation name of the project on github.
     */
    private static String determineGitHubProjectOrganisation(final File projectDir) throws Exception
    {
        final Git git = Git.open(projectDir);
        final String repositoryPath = git.remoteList().call().get(0).getURIs().get(0).getPath();

        final String[] pathMembers = repositoryPath.split("/");
        return pathMembers[pathMembers.length - 2];
    }
}
