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
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class ExtractTeamCityProjectConfigurationTask extends DefaultTask
{

    public ExtractTeamCityProjectConfigurationTask()
    {
        getDestination().convention(getProject().getRootProject().getLayout().getProjectDirectory().dir(getProject().provider(() -> "./")));
        getShouldAutoCommit().convention(true);
        getRequiresCleanWorkspace().convention(true);

        setGroup("publishing");
        setDescription("Creates (or recreates) a default TeamCity project configuration directory for use with the MinecraftForge TeamCity server.");
    }

    @InputDirectory
    @PathSensitive(PathSensitivity.NONE)
    public abstract DirectoryProperty getDestination();

    @Input
    public abstract Property<Boolean> getShouldAutoCommit();

    @Input
    public abstract Property<Boolean> getRequiresCleanWorkspace();

    @TaskAction
    public void run() throws Exception
    {
        final File destDir = getDestination().getAsFile().get();
        final File teamcityDir = new File(destDir, ".teamcity");

        if (getRequiresCleanWorkspace().get())
        {
            CheckForCleanWorkspace(destDir);
        }

        String fileZip = ExportResource();

        if (teamcityDir.exists())
        {
            if (!teamcityDir.delete())
            {
                throw new IllegalStateException("Could not delete the existing .teamcity project directory!");
            }
        }

        ExtractTeamCityZip(fileZip, destDir);
        ReplaceTeamCityTestProjectIds(destDir);

        if (getShouldAutoCommit().get())
        {
            CommitTeamCityProjectDirectory(destDir);
        }
    }

    private static void ExtractTeamCityZip(final String fileZip, final File destDir) throws Exception
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

    private static String ExportResource() throws Exception
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

    private static void ReplaceTeamCityTestProjectIds(final File projectDir) throws Exception
    {
        final String projectId = DetermineGitHubProjectName(projectDir);
        final File teamcityDir = new File(projectDir, ".teamcity");
        if (!teamcityDir.exists())
        {
            return;
        }

        for (final File file : Objects.requireNonNull(teamcityDir.listFiles()))
        {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            content = content.replaceAll("TeamCityTest", projectId);
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String DetermineGitHubProjectName(final File projectDir) throws Exception
    {
        final Git git = Git.open(projectDir);
        final String repositoryPath = git.remoteList().call().get(0).getURIs().get(0).getPath();

        return repositoryPath.substring(repositoryPath.lastIndexOf("/") + 1).replace(".git", "");
    }

    private static void CheckForCleanWorkspace(final File projectDir) throws Exception {
        final Git git = Git.open(projectDir);
        final Status status = git.status().call();
        if (!status.isClean()) {
            throw new Exception("Workspace is not clean. Please commit your changes and try again.");
        }
    }

    private static void CommitTeamCityProjectDirectory(final File projectDir) throws Exception {
        final Git git = Git.open(projectDir);
        git.add().addFilepattern("./teamcity/*").setUpdate(false).call();
        git.commit().setMessage("Added or updated support for building the project in TeamCity.").call();
    }
}
