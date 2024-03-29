/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.changelog

import net.minecraftforge.gradleutils.GradleUtils;
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.util.SystemReader;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*

abstract class GenerateChangelog extends DefaultTask {
    GenerateChangelog() {
        //Setup defaults: Using merge-base based text changelog generation of the local project into build/changelog.txt
        getGitDirectory().convention(getProject().getLayout().getProjectDirectory().dir(".git"));
        getBuildMarkdown().convention(false);
        getOutputFile().convention(getProject().getLayout().getBuildDirectory().file("changelog.txt"));
    }

    @OutputFile
    abstract RegularFileProperty getOutputFile();

    @InputDirectory
    @PathSensitive(PathSensitivity.NONE)
    abstract DirectoryProperty getGitDirectory();

    @Input
    abstract Property<Boolean> getBuildMarkdown();

    @Input
    @Optional
    abstract Property<String> getStart();

    @Input
    @Optional
    abstract Property<String> getProjectUrl();

    @TaskAction
    void exec() throws IOException {

        String changelog = ""
        def parent = SystemReader.instance
        SystemReader.instance = new GradleUtils.DisableSystemConfig(parent)
        try(Git git = Git.open(getGitDirectory().getAsFile().get())) {
            def url = projectUrl.getOrNull()
            if (url == null)
                GradleUtils.buildProjectUrl(git);

            String start = getStart().getOrNull();
            RevCommit from = null;

            if (start == null) {
                from = ChangelogUtils.getMergeBaseCommit(git); //Grab the last merge base commitHash on the current branch.
                //Deal with a single branch repository without merge-base
                if (from == null)
                    from = ChangelogUtils.getFirstCommitInRepository(git);
            } else {
                def tags = ChangelogUtils.getTagToCommitMap(git)
                def commitHash = tags.get(start)
                if (commitHash == null)
                    commitHash = start

                from = ChangelogUtils.getCommitFromId(git, ObjectId.fromString(commitHash))
            }

            def head = ChangelogUtils.getHead(git)
            changelog = ChangelogUtils.generateChangelogFromTo(git, url, !buildMarkdown.get(), from, head)
        } finally {
            SystemReader.instance = parent
        }

        var file = outputFile.asFile.get()
        if (!file.parentFile.exists())
            file.parentFile.mkdirs()

        file.setText(changelog, 'UTF8')
    }
}
