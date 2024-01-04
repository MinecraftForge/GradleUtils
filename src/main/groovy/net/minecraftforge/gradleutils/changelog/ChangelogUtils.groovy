/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.changelog

import groovy.transform.PackageScope
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.errors.MissingObjectException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin

import java.util.function.Function
import java.util.regex.Pattern

@PackageScope
class ChangelogUtils {
    private static final String TASK_NAME = "createChangelog";
    private static final String COPY_TASK_NAME = "copyChangelog";

    /**
     * Generates a changelog string that can be written to a file from a given git directory and repository url.
     * The changes will be generated from the given commit to the given commit.
     *
     * @param projectDirectory The directory from which to pull the git commit information.
     * @param repositoryUrl The github url of the repository.
     * @param justText Indicates if plain text ({@code true}) should be used, or changelog should be used ({@code false}).
     * @param commitHash The commit hash of the commit to use as the beginning of the changelog.
     * @param endCommitHash The commit hash of the commit to use as the end of the changelog.
     * @return A multiline changelog string.
     */
    static String generateChangelogFromTo(final Git git, final String repositoryUrl, final boolean justText, final RevCommit start, final RevCommit end) {
        def endCommitHash = end.toObjectId().getName(); //Grab the commit hash of the end commit.
        def startCommitHash = start.toObjectId().getName(); //Grab the commit hash of the start commit.

        def changeLogName = git.repository.fullBranch; //Generate a changelog name from the current branch.
        if (changeLogName != null) {
            changeLogName = changeLogName.replace("refs/heads/", ""); //Replace the heads prefix with nothing to only get the name of the current branch.
        }

        def log = getCommitLogFromTo(git, start, end); //Get all commits between the start and the end.
        def logList = log.toList(); //And generate a list from it.

        def tagMap = getCommitToTagMap(git); //Grab a map between commits and tag names.
        def versionMap = buildVersionMap(logList, tagMap); //And generate a version map from this. Mapping each commit to a unique version.
        def primaryVersionMap = getPrimaryVersionMap(logList, tagMap); //Then determine which commits belong to which identifiable-version mappings.

        //Determine the length of each identifiable-versions max-length commit specific version.
        //(How wide does the area in-front of the commit message need to be to fit all versions in the current identifiable-version?)
        def primaryVersionPrefixLengthMap = determinePrefixLengthPerPrimaryVersion(versionMap.values(), new HashSet<String>(primaryVersionMap.values()));

        //Generate the header
        def changelog ="### [$changeLogName Changelog]($repositoryUrl/compare/$startCommitHash...$endCommitHash)\n"
        //If we are not generating markdown, redo it without the damn url.
        if (justText)
            changelog = "$changeLogName Changelog\n"

        //Some working variables and processing patterns.
        def currentPrimaryVersion = "" //The current identifiable-version.
        def pullRequestPattern = Pattern.compile("\\(#(?<pullNumber>[0-9]+)\\)"); //A Regex pattern to find PullRequest numbers in commit messages.

        //Loop over all commits and append their message as a changelog.
        //(They are already in order from newest to oldest, so that works out for us.)
        for(final RevCommit commit : logList) {
            def commitHash = commit.toObjectId().name(); //Get the commit hash, so we can look it up in maps.

            def requiresVersionHeader = false; //Indicates later on if we need to inject a new version header.
            if (primaryVersionMap.containsKey(commitHash)) {
                def versionsPrimaryVersion = primaryVersionMap.get(commitHash); //The current commits primary version.
                requiresVersionHeader = versionsPrimaryVersion != currentPrimaryVersion; //Check if we need a new one.
                currentPrimaryVersion = versionsPrimaryVersion; //Update the cached version.
            }

            //Generate a version header if required.
            if (requiresVersionHeader && justText) {
                def header = currentPrimaryVersion;
                def headerMarker = header.replaceAll(".", "=")

                changelog += "$header\n"
                changelog += "$headerMarker\n"
            }

            //Generate the commit message prefix.
            def commitHeader = " - "
            if (versionMap.containsKey(commitHash)) {
                def version = versionMap.get(commitHash);
                if (tagMap.containsKey(commitHash) && !justText) {
                    commitHeader+="[${version.padRight(primaryVersionPrefixLengthMap.getOrDefault(currentPrimaryVersion, 0))}]($repositoryUrl/tree/$version)"
                }
                else
                {
                    commitHeader+="${version.padRight(primaryVersionPrefixLengthMap.getOrDefault(currentPrimaryVersion, 0))}"
                }
            }


            def commitHeaderLength = commitHeader.length();
            commitHeader += " "
            def noneCommitHeaderPrefix = String.join("", Collections.nCopies(commitHeaderLength," ")) + " "; //Generate a prefix for each line in the commit message so that it lines up.

            //Get a processed commit message body.
            def subject = processCommitBody(commit.getFullMessage().trim());

            //If we generate changelog, then process the pull request numbers.
            if (!justText){
                //Check if we have a pull request.
                def matcher = pullRequestPattern.matcher(subject);
                if (matcher.find()) {
                    //Grab the number
                    def pullRequestNumber = matcher.group("pullNumber");

                    //Replace the pull request number.
                    subject = subject.replace("#$pullRequestNumber", "[#$pullRequestNumber]($repositoryUrl/pull/$pullRequestNumber)")
                }
            }

            //Replace each newline in the message with a newline and a prefix so the message lines up.
            subject = subject.replaceAll("\\n", "\n" + noneCommitHeaderPrefix);

            //Append the generated entry with its header (list entry + version number)
            changelog += "$commitHeader$subject"
            changelog += '\n'

            //When we are done writing the last entry, add a newline.
            if (tagMap.containsKey(commitHash) && justText)
                changelog += "\n"
        }

        return changelog;
    }

    /**
     * Finds the youngest merge base commit on the current branch.
     *
     * @param git The git workspace to find the merge base in.
     * @return The merge base commit or null.
     */
    static RevCommit getMergeBaseCommit(final Git git) {
        def headCommit = getHead(git);
        def remoteBranches = getAvailableRemoteBranches(git);
        return remoteBranches.stream()
                .filter(branch -> branch.getObjectId().getName() != headCommit.toObjectId().getName())
                .map(branch -> getMergeBase(git, branch))
                .filter(revCommit -> (revCommit !== null) &&
                        (revCommit.toObjectId().getName() != headCommit.toObjectId().getName()))
                .sorted(Comparator.comparing(new Function<RevCommit, Integer>() {
                    @Override
                    Integer apply(final RevCommit revCommit) {
                        return Integer.MAX_VALUE - revCommit.getCommitTime();
                    }
                }))
                .findFirst()
                .orElse(null)
    }

    /**
     * Get all available remote branches in the git workspace.
     *
     * @param git The git workspace to get the branches from.
     * @return A list of remote branches.
     */
    private static List<Ref> getAvailableRemoteBranches(final Git git) {
        def command = git.branchList();
        command.listMode = ListBranchCommand.ListMode.REMOTE
        return command.call();
    }

    /**
     * Get the merge base commit between the current and the given branch.
     *
     * @param git The git workspace to get the merge base in.
     * @param other The other branch to find the merge base with.
     * @return A merge base commit or null.
     */
    private static RevCommit getMergeBase(final Git git, final Ref other) {
        try (RevWalk walk = new RevWalk(git.repository)) {
            walk.setRevFilter(RevFilter.MERGE_BASE);
            walk.markStart(getCommitFromRef(git, other));
            walk.markStart(getHead(git));

            RevCommit mergeBase = null;
            RevCommit current;
            while ((current = walk.next()) != null) {
                mergeBase = current;
            }
            return mergeBase;
        } catch (MissingObjectException ignored) {
            return null;
        }
    }

    /**
     * Gets the head commit of the given git workspace.
     *
     * @param git The git workspace to get the head commit from.
     * @return The head commit.
     */
    static RevCommit getHead(final Git git) {
        def headId = git.repository.resolve(Constants.HEAD);
        return getCommitFromId(git, headId);
    }

    /**
     * Determines the commit that the given ref references.
     *
     * @param git The git workspace to get the commit from.
     * @param other The reference to get the commit for.
     * @return The commit referenced by the given reference in the given git workspace.
     */
    private static RevCommit getCommitFromRef(final Git git, final Ref other) {
        try (RevWalk revWalk = new RevWalk(git.repository)) {
            return revWalk.parseCommit(other.objectId);
        }
    }

    /**
     * Determines the commit that the given object references.
     *
     * @param git The git workspace to get the commit from.
     * @param other The object to get the commit for.
     * @return The commit referenced by the given object in the given git workspace.
     */
    static RevCommit getCommitFromId(final Git git, final ObjectId other) {
        try (RevWalk revWalk = new RevWalk(git.repository)) {
            return revWalk.parseCommit(other);
        }
    }

    /**
     * Gets the commit message from the start commit to the end.
     * Returns it in youngest to oldest order (so from end to start).
     *
     * @param git The git workspace to get the commits from.
     * @param start The start commit (the oldest).
     * @param end The end commit (the youngest).
     * @return The commit log.
     */
    private static Iterable<RevCommit> getCommitLogFromTo(final Git git, final RevCommit start, final RevCommit end) {
        def log = git.log().add(end)

        // If our starting commit contains at least one parent (it is not the 'root' commit), exclude all of those parents
        for (RevCommit parent : start.getParents()) {
            log.not(parent)
        }
        // We do not exclude the starting commit itself, so the commit is present in the returned iterable

        return log.call()
    }

    /**
     * Builds a map of commit hashes to tag names.
     *
     * @param git The git workspace to get the tags from.
     * @return The commit hashes to tag map.
     */
    private static Map<String, String> getCommitToTagMap(final Git git) {
        final Map<String, String> versionMap = new HashMap<>();
        for(Ref tag : git.tagList().call()) {
            ObjectId tagId = git.getRepository().getRefDatabase().peel(tag).peeledObjectId ?: tag.objectId;
            versionMap.put(tagId.name(), tag.getName().replace(Constants.R_TAGS, ""))
        }

        return versionMap;
    }

    /**
     * Builds a map of tag name to commit hash.
     *
     * @param git The git workspace to get the tags from.
     * @return The tags to commit hash map.
     */
    static Map<String, String> getTagToCommitMap(final Git git) {
        final Map<String, String> versionMap = new HashMap<>();
        for(Ref tag : git.tagList().call()) {
            ObjectId tagId = git.getRepository().getRefDatabase().peel(tag).peeledObjectId ?: tag.objectId;
            versionMap.put(tag.getName().replace(Constants.R_TAGS, ""), tagId.name());
        }

        return versionMap;
    }

    /**
     * Builds a commit hash to version map.
     * The commits version is build based on forges common version scheme.
     *
     * From the current identifiable-version (in the form of major.minor) a patch section is appended based on the amount
     * of commits since the last tagged commit. A tagged commit get 0 patch section appended.
     * Any commits that are before the first tagged commit will not get a patch section append but
     * a '-pre-' section will be appended, with a commit count as well.
     *
     * @param commits The commits to build the version map for.
     * @param commitHashToVersions A commit hash to identifiable-version name map.
     * @return The commit hash to calculated version map.
     */
    private static Map<String, String> buildVersionMap(final List<RevCommit> commits, final Map<String, String> commitHashToVersions) {
        //Determine the version that sets the first fixed version commit.
        def prereleaseTargetVersion = getFirstReleasedVersion(commits, commitHashToVersions);
        //Inverse all commits (Now from old to new).
        def reversedCommits = commits.reverse();

        //Working variables to keep track of the current version and the offset.
        String currentVersion = "";
        int offset = 0;

        //Map to store the results.
        Map<String, String> versionMap = new HashMap<>();
        for(RevCommit commit : reversedCommits) {
            //Grab the commit hash.
            def commitHash = commit.toObjectId().name();
            def version = commitHashToVersions.get(commitHash); //Check if we have a tagged commit for a specific identifiable-version.
            if (version != null) {
                //We have a tagged commit, update the current version and set the offset to 0.
                offset = 0;
                currentVersion = version;
            }
            else
            {
                //We don't have a tagged commit, increment the offset.
                offset++;
            }

            //Determine the commits version.
            def releasedVersion = currentVersion + "." + offset;
            if (currentVersion.isEmpty()) {
                //We do not have a tagged commit yet.
                //So append the pre-release offset to the version
                releasedVersion = prereleaseTargetVersion + "-pre-$offset"
            }
            versionMap.put(commitHash, releasedVersion);
        }

        return versionMap;
    }

    /**
     * Finds the oldest version in the list of commits.
     *
     * @param commits The commits to check. (youngest to oldest)
     * @param commitHashToVersions The commit hash to version map.
     * @return The oldest identifiable-version in the list of commits.
     */
    private static String getFirstReleasedVersion(final List<RevCommit> commits, final Map<String, String> commitHashToVersions) {
        String currentVersion = "1.0";
        //Simple loop over all commits (natural order is youngest to oldest)
        for(RevCommit commit : commits) {
            def commitHash = commit.toObjectId().name();
            def version = commitHashToVersions.get(commitHash);
            if (version != null) {
                currentVersion = version;
            }
        }

        //Return the last one found.
        return currentVersion;
    }

    /**
     * Builds a map that matches a commit hash to an identifiable-version (the primary version).
     *
     * @param commits The commits to check from youngest to oldest.
     * @param commitHashToVersions A commit hash to identifiable-version name map.
     * @return The commit hash to identifiable-version map.
     */
    private static Map<String, String> getPrimaryVersionMap(final List<RevCommit> commits, final Map<String, String> commitHashToVersions) {
        def lastVersion = null;
        List<String> currentVersionCommitHashes = new ArrayList<>();
        Map<String, String> primaryVersionMap = new HashMap<>();

        //Loop over all commits.
        for(RevCommit commit : commits) {
            def commitHash = commit.toObjectId().name();
            currentVersionCommitHashes.add(commitHash); //Collect all commit hashes in the current identifiable version.
            def version = commitHashToVersions.get(commitHash);
            if (version != null) {
                //We found a version boundary (generally a tagged commit is the first build for a given identifiable-version).
                for (String combinedHash : currentVersionCommitHashes) {
                    primaryVersionMap.put(combinedHash, version);
                    lastVersion = version;
                }

                //Reset the collection list.
                currentVersionCommitHashes.clear()
            }
        }

        //We need to deal with repositories without properly tagged versions
        //They are all 1.0-pre-x for now then.
        if (commitHashToVersions.isEmpty())
        {
            lastVersion = "1.0"
        }

        if (lastVersion != null) {
            //Everything that is left over are pre-releases.
            for (String combinedHash : currentVersionCommitHashes) {
                primaryVersionMap.put(combinedHash, lastVersion + "-pre");
            }
        }

        //Return the collected data.
        return primaryVersionMap;
    }

    /**
     * Determine the length of pre commit message prefix for each identifiable-version.
     * This is generally dependent on the amount of releases in each window, more releases means more characters, and this a longer prefix.
     * The prefix length guarantees that all versions in that window will fit in the log, lining up the commit messages vertically under each other.
     *
     * @param availableVersions The available versions to check. Order does not matter.
     * @param availablePrimaryVersions The available primary versions to check. Order does not matter.
     * @return A map from primary identifiable-version to prefix length.
     */
    private static Map<String, Integer> determinePrefixLengthPerPrimaryVersion(final Collection<String> availableVersions, final Set<String> availablePrimaryVersions) {
        Map<String, Integer> result = new HashMap<>();

        //Sort the versions reversely alphabetically by length (reverse alphabetical order).
        //Needed so that versions which prefix another version are tested later then the versions they are an infix for.
        ArrayList<String> sortedVersions = new ArrayList<>(availablePrimaryVersions);
        Collections.sort(sortedVersions);
        List<String> workingPrimaryVersions = sortedVersions.reverse();

        //Loop over each known version.
        for(String version : availableVersions) {
            //Check all primary versions for a prefix match.
            for(String primaryVersion : workingPrimaryVersions) {
                if (!version.startsWith(primaryVersion)) {
                    continue;
                }

                //Check if we have a longer version, if so store.
                def length = version.trim().length();
                if (!result.containsKey(primaryVersion) || result.get(primaryVersion) < length)
                    result.put(primaryVersion, length);

                //Abort the inner loop and continue with the next.
                break;
            }
        }

        return result;
    }

    /**
     * Processes the commit body of a commit stripping out unwanted information.
     *
     * @param body The body to process.
     * @return The result of the processing.
     */
    private static String processCommitBody(final String body) {
        final String[] bodyLines = body.split("\n"); //Split on newlines.
        final List<String> resultingLines = new ArrayList<>();
        for(String bodyLine : bodyLines) {
            if (bodyLine.startsWith("Signed-off-by: ")) //Remove all the signed of messages.
                continue;

            if (bodyLine.trim().isEmpty()) //Remove empty lines.
                continue;

            resultingLines.add(bodyLine)
        }

        return String.join("\n", resultingLines).trim(); //Join the result again.
    }

    static RevCommit getFirstCommitInRepository(final Git git) {
        final Iterable<RevCommit> commits = git.log().call();
        final List<RevCommit> commitList = commits.toList();

        if (commitList.isEmpty())
            return null;

        return commitList.get(commitList.size() - 1);
    }

    /**
     * Adds the createChangelog task to the target project.
     * Also exposes it as a artifact of the 'createChangelog' configuration.
     * As this is the
     * <a href="https://docs.gradle.org/current/samples/sample_cross_project_output_sharing.html">
     * recommended way</a> to share task outputs between multiple projects
     *
     * @param project Project to add the task to
     * @return The task responsible for generating the changelog
     */
    static TaskProvider<GenerateChangelog> setupChangelogTask(Project project) {
        var ret = project.tasks.register(TASK_NAME, GenerateChangelog)

        project.configurations.register(TASK_NAME) {
            canBeResolved = false
        }

        project.artifacts.add(TASK_NAME, ret)

        def assembleTask = LifecycleBasePlugin.ASSEMBLE_TASK_NAME
        project.plugins.withType(LifecycleBasePlugin).configureEach {
            project.tasks.named(assembleTask).configure {
                dependsOn(ret)
            }
        }

        return ret
    }

    /**
     * Sets up the changelog generation on all maven publications in the project.
     * It also sets up publishing for all subprojects as long as that subproject does not
     * have another changelog plugin overriding the propagation
     *
     * @param project The project to add changelog generation publishing to.
     */
    static void setupChangelogGenerationOnAllPublishTasks(Project project) {
        setupChangelogGenerationForAllPublications(project)
        project.subprojects.forEach {sub ->
            sub.afterEvaluate {
                def ext = sub.extensions.findByType(ChangelogExtension)
                def parent = project
                while (ext == null && parent != null) {
                    ext = parent.extensions.findByType(ChangelogExtension)
                    parent = parent.parent == parent ? null : parent.parent
                }
                if (ext != null && ext.publishAll)
                    setupChangelogGenerationForAllPublications(sub)
            }
        }
    }

    private static void setupChangelogGenerationForAllPublications(Project project) {
        var ext = project.getExtensions().findByName("publishing") as PublishingExtension
        if (ext == null)
            return

        //Get each extension and add the publishing task as a publishing artifact.
        ext.publications.withType(MavenPublication).configureEach {
            //Add the task as a publishing artifact.
            setupChangelogGenerationForPublishing(project, it)
        }
    }

    private static ChangelogExtension findParent(Project project) {
        var ext = project.extensions.findByType(ChangelogExtension)
        if (ext?.task != null)
            return ext

        var parent = project.getParent() == project ? null : project.getParent()
        return parent == null ? null : findParent(parent)
    }

    /**
     * The recommended way to share task outputs across projects is to export them as dependencies
     * So for any project that doesn't generate the changelog directly, we must create a copy task
     * and new configuration
     */
    private static TaskProvider<Task> findChangelogTask(final Project project) {
        // See if we've already made the task
        if (project.tasks.names.contains(TASK_NAME))
            return project.tasks.named(TASK_NAME)

        if (project.tasks.names.contains(COPY_TASK_NAME))
            return project.tasks.named(COPY_TASK_NAME)

        // See if there is any parent with a changelog configured
        var parent = findParent(project)
        if (parent == null)
            return null

        var cfg = project.configurations.register(COPY_TASK_NAME) {
            canBeConsumed = false
        }

        project.dependencies.add(COPY_TASK_NAME, project.dependencies.project('path': parent.project.path, 'configuration': TASK_NAME))

        var task = project.tasks.register(COPY_TASK_NAME, CopyChangelog) {
            configuration = cfg.get()
        }

        return task
    }

    /**
     * Sets up the changelog generation on the given maven publication.
     *
     * @param project The project in question.
     * @param publication The publication in question.
     */
    static void setupChangelogGenerationForPublishing(final Project project, final MavenPublication publication) {
        try {
            //After evaluation run the publishing modifier.
            project.afterEvaluate({
                setupChangelogGenerationForPublishingAfterEvaluation(project, publication)
            })
        }
        catch (InvalidUserCodeException ignored) {
            //Already in after evaluate.
            setupChangelogGenerationForPublishingAfterEvaluation(project, publication)
        }
    }

    private static void setupChangelogGenerationForPublishingAfterEvaluation(final Project project, final MavenPublication publication) {
        boolean existing = !publication.artifacts.findAll { it.classifier == 'changelog' && it.extension == 'txt'}.isEmpty()
        if (existing)
            return

        //Grab the task
        var task = findChangelogTask(project);

        //Add a new changelog artifact and publish it.
        publication.artifact(task.get().outputs.files.singleFile) {
            builtBy(task)
            classifier = 'changelog'
            extension = 'txt'
        }
    }
}
