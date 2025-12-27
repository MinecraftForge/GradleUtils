/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.internal;

import com.github.benmanes.gradle.versions.reporter.Reporter;
import com.github.benmanes.gradle.versions.reporter.result.DependenciesGroup;
import com.github.benmanes.gradle.versions.reporter.result.Dependency;
import com.github.benmanes.gradle.versions.reporter.result.DependencyLatest;
import com.github.benmanes.gradle.versions.reporter.result.DependencyOutdated;
import com.github.benmanes.gradle.versions.reporter.result.DependencyUnresolved;
import com.github.benmanes.gradle.versions.reporter.result.Result;
import com.github.benmanes.gradle.versions.updates.gradle.GradleReleaseChannel;
import com.github.benmanes.gradle.versions.updates.gradle.GradleUpdateResult;
import com.github.benmanes.gradle.versions.updates.gradle.GradleUpdateResults;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class EnhancedVersionReporter {
    record EnhancedResult(
        int count,
        DependenciesGroup<Dependency> current,
        EnhancedDependenciesGroup<EnhancedDependencyOutdated> outdated,
        EnhancedDependenciesGroup<EnhancedDependencyLatest> exceeded,
        DependenciesGroup<Dependency> undeclared,
        DependenciesGroup<DependencyUnresolved> unresolved,
        EnhancedGradleUpdateResults gradle
    ) {
        EnhancedResult(Result result, String revision) {
            this(
                result.getCount(),
                result.getCurrent(),
                new EnhancedDependenciesGroup<>(result.getOutdated().getDependencies().stream().map(d -> new EnhancedDependencyOutdated(d, revision)).collect(Collectors.toUnmodifiableSet())),
                new EnhancedDependenciesGroup<>(result.getExceeded().getDependencies().stream().map(EnhancedDependencyLatest::new).collect(Collectors.toUnmodifiableSet())),
                result.getUndeclared(),
                result.getUnresolved(),
                new EnhancedGradleUpdateResults(result.getGradle())
            );
        }
    }

    private record EnhancedDependenciesGroup<T>(
        int count,
        Set<T> dependencies
    ) {
        private EnhancedDependenciesGroup(Set<T> dependencies) {
            this(dependencies.size(), dependencies);
        }
    }

    private record EnhancedDependencyOutdated(
        String group,
        String name,
        String version,
        String projectUrl,
        String userReason,
        String available,
        Supplier<String> diffUrl
    ) {
        private EnhancedDependencyOutdated(DependencyOutdated dependency, String revision) {
            this(
                dependency.getGroup(),
                dependency.getName(),
                dependency.getVersion(),
                dependency.getProjectUrl(),
                dependency.getUserReason(),
                dependency.getAvailable().get(revision),
                getDiffUrl(dependency, dependency.getVersion(), dependency.getAvailable().get(revision))
            );
        }
    }

    private record EnhancedDependencyLatest(
        String group,
        String name,
        String version,
        String projectUrl,
        String userReason,
        String latest,
        Supplier<String> diffUrl
    ) {
        private EnhancedDependencyLatest(DependencyLatest dependency) {
            this(
                dependency.getGroup(),
                dependency.getName(),
                dependency.getVersion(),
                dependency.getProjectUrl(),
                dependency.getUserReason(),
                dependency.getLatest(),
                getDiffUrl(dependency, dependency.getVersion(), dependency.getLatest())
            );
        }
    }

    private record EnhancedGradleUpdateResults(
        boolean enabled,
        GradleUpdateResult running,
        GradleUpdateResult current,
        GradleUpdateResult releaseCandidate,
        GradleUpdateResult nightly,
        String currentReleaseNotesUrl,
        String releaseCandidateReleaseNotesUrl,
        String nightlyReleaseNotesUrl
    ) {
        private EnhancedGradleUpdateResults(
            boolean enabled,
            GradleUpdateResult running,
            GradleUpdateResult current,
            GradleUpdateResult releaseCandidate,
            GradleUpdateResult nightly
        ) {
            this(
                enabled,
                running,
                current,
                releaseCandidate,
                nightly,
                "https://docs.gradle.org/%s/release-notes.html".formatted(current.getVersion()),
                "https://docs.gradle.org/%s/release-notes.html".formatted(releaseCandidate.getVersion()),
                "https://docs.gradle.org/%s/release-notes.html".formatted(nightly.getVersion())
            );
        }

        //https://docs.gradle.org/9.0.0/release-notes.html
        private EnhancedGradleUpdateResults(GradleUpdateResults dependency) {
            this(
                dependency.getEnabled(),
                dependency.getRunning(),
                dependency.getCurrent(),
                dependency.getReleaseCandidate(),
                dependency.getNightly()
            );
        }
    }

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final String[] KNOWN_VERSION_PREFIXES = {"", "v", "release-"};

    private static Supplier<@Nullable String> getDiffUrl(Dependency dependency, String currentVersion, String latestVersion) {
        var projectUrl = dependency.getProjectUrl();
        if (projectUrl == null || !projectUrl.contains("github.com/")) {
            return () -> null;
        }

        var requests = new ArrayList<CompletableFuture<String>>(KNOWN_VERSION_PREFIXES.length);
        for (var prefix : KNOWN_VERSION_PREFIXES) {
            var diffUrl = (projectUrl.endsWith("/") ? projectUrl.substring(0, projectUrl.length() - 1) : projectUrl)
                + "/compare/%s...%s".formatted(prefix + currentVersion, prefix + latestVersion);

            var request = HttpRequest.newBuilder(URI.create(diffUrl)).GET().build();
            requests.add(HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding()).thenApply(response ->
                response.statusCode() == 200 ? diffUrl : null
            ));
        }

        return () -> {
            for (var request : requests) {
                var diffUrl = request.join();
                if (diffUrl != null) return diffUrl;
            }

            return null;
        };
    }

    private static String label(Dependency dependency) {
        return "%s:%s".formatted(Objects.requireNonNullElse(dependency.getGroup(), ""), dependency.getName());
    }

    private static String label(EnhancedDependencyOutdated dependency) {
        return "%s:%s".formatted(Objects.requireNonNullElse(dependency.group, ""), dependency.name);
    }

    private static String label(EnhancedDependencyLatest dependency) {
        return "%s:%s".formatted(Objects.requireNonNullElse(dependency.group, ""), dependency.name);
    }

    record PlainTextDependencyReporter(
        String projectPath, String revision, String gradleReleaseChannel
    ) implements Reporter {
        @Override
        public String getFileExtension() {
            return "txt";
        }

        @Override
        public void write(OutputStream outputStream, Result result) {
            this.write(outputStream, new EnhancedResult(result, this.revision));
        }

        void write(OutputStream outputStream, EnhancedResult result) {
            this.generate(
                outputStream instanceof PrintStream printStream ? printStream : new PrintStream(outputStream),
                result
            );
        }

        private void generate(PrintStream printStream, EnhancedResult result) {
            this.writeHeader(printStream);

            if (result.count == 0) {
                printStream.println();
                printStream.println("No dependencies found.");
            } else {
                this.writeUpToDate(printStream, result);
                this.writeExceedLatestFound(printStream, result);
                this.writeUpgrades(printStream, result);
                this.writeUndeclared(printStream, result);
                this.writeUnresolved(printStream, result);
            }

            this.writeGradleUpdates(printStream, result);
        }

        private void writeHeader(PrintStream printStream) {
            printStream.println();
            printStream.println("------------------------------------------------------------");
            printStream.printf("%s Project Dependency Updates (report to plain text file)%n", this.projectPath);
            printStream.println("------------------------------------------------------------");
        }

        private void writeUpToDate(PrintStream printStream, EnhancedResult result) {
            var upToDateVersions = result.current.getDependencies();
            if (upToDateVersions.isEmpty()) return;

            printStream.println();
            printStream.printf("The following dependencies are using the latest %s version:%n", this.revision);
            for (var dependency : upToDateVersions) {
                printStream.printf(" - %s:%s%n", label(dependency), dependency.getVersion());
                if (dependency.getUserReason() != null) {
                    printStream.printf("     %s%n", dependency.getUserReason());
                }
            }
        }

        private void writeExceedLatestFound(PrintStream printStream, EnhancedResult result) {
            var downgradeVersions = result.exceeded.dependencies;
            if (downgradeVersions.isEmpty()) return;

            printStream.println();
            printStream.printf("The following dependencies exceed the version found at the %s revision level:%n", this.revision);
            for (var dependency : downgradeVersions) {
                var currentVersion = dependency.version;
                var latestVersion = dependency.latest;
                printStream.printf(" - %s [%s <- %s]%n", label(dependency), currentVersion, latestVersion);
                if (dependency.userReason != null) {
                    printStream.printf("     %s%n", dependency.userReason);
                }
                if (dependency.projectUrl != null) {
                    printStream.printf("     %s%n", dependency.projectUrl);
                }
                var diffUrl = dependency.diffUrl.get();
                if (diffUrl != null) {
                    printStream.printf("     %s%n", diffUrl);
                }
            }
        }

        private void writeUpgrades(PrintStream printStream, EnhancedResult result) {
            var upgradeVersions = result.outdated.dependencies;
            if (upgradeVersions.isEmpty()) return;

            printStream.println();
            printStream.printf("The following dependencies have later %s versions:%n", this.revision);
            for (var dependency : upgradeVersions) {
                var currentVersion = dependency.version;
                var latestVersion = dependency.available;
                printStream.printf(" - %s [%s -> %s]%n", label(dependency), currentVersion, latestVersion);
                if (dependency.userReason != null) {
                    printStream.printf("     %s%n", dependency.userReason);
                }
                if (dependency.projectUrl != null) {
                    printStream.printf("     %s%n", dependency.projectUrl);
                }
                var diffUrl = dependency.diffUrl.get();
                if (diffUrl != null) {
                    printStream.printf("     %s%n", diffUrl);
                }
            }
        }

        private void writeUndeclared(PrintStream printStream, EnhancedResult result) {
            var undeclaredVersions = result.undeclared.getDependencies();
            if (undeclaredVersions.isEmpty()) return;

            printStream.println();
            printStream.println("Failed to compare versions for the following dependencies because they were declared without version:");
            for (var dependency : undeclaredVersions) {
                printStream.printf(" - %s%n", label(dependency));
            }
        }

        private void writeUnresolved(PrintStream printStream, EnhancedResult result) {
            var unresolved = result.unresolved.getDependencies();
            if (unresolved.isEmpty()) return;

            printStream.println();
            printStream.println("Failed to determine the latest version for the following dependencies:");
            for (var dependency : unresolved) {
                printStream.printf(" - %s%n", label(dependency));
                if (dependency.getUserReason() != null) {
                    printStream.printf("     %s%n", dependency.getUserReason());
                }
                if (dependency.getProjectUrl() != null) {
                    printStream.printf("     %s%n", dependency.getProjectUrl());
                }
                System.err.println("WARNING: Failed to determine the latest version for " + label(dependency));
                System.err.println(dependency.getReason().indent(2));
            }
        }

        private void writeGradleUpdates(PrintStream printStream, EnhancedResult result) {
            if (!result.gradle.enabled) return;

            printStream.println();
            printStream.printf("Gradle %s updates:%n", this.gradleReleaseChannel);
            // Log Gradle update checking failures.
            if (result.gradle.current.isFailure()) {
                printStream.printf("ERROR: [release channel: %s] %s%n", GradleReleaseChannel.CURRENT.getId(), result.gradle.current.getReason());
            }
            if ((GradleReleaseChannel.RELEASE_CANDIDATE.getId().equals(this.gradleReleaseChannel) || GradleReleaseChannel.NIGHTLY.getId().equals(this.gradleReleaseChannel))
                && result.gradle.releaseCandidate.isFailure()) {
                printStream.printf("[ERROR] [release channel: %s] %s%n", GradleReleaseChannel.RELEASE_CANDIDATE.getId(), result.gradle.releaseCandidate.getReason());
            }
            if (GradleReleaseChannel.NIGHTLY.getId().equals(this.gradleReleaseChannel)
                && result.gradle.nightly.isFailure()) {
                printStream.printf("[ERROR] [release channel: %s] %s%n", GradleReleaseChannel.RELEASE_CANDIDATE.getId(), result.gradle.nightly.getReason());
            }

            // print Gradle updates in breadcrumb format
            printStream.printf(" - Gradle: [%s", result.gradle.running().getVersion());
            String updatePrinted = null;
            if (result.gradle.current.isUpdateAvailable() && result.gradle.current.compareTo(result.gradle.running()) > 0) {
                updatePrinted = result.gradle.currentReleaseNotesUrl;
                printStream.printf(" -> %s", result.gradle.current.getVersion());
            }
            if ((GradleReleaseChannel.RELEASE_CANDIDATE.getId().equals(this.gradleReleaseChannel) || GradleReleaseChannel.NIGHTLY.getId().equals(this.gradleReleaseChannel)) &&
                result.gradle.releaseCandidate.isUpdateAvailable() &&
                result.gradle.releaseCandidate.compareTo(result.gradle.current) > 0
            ) {
                updatePrinted = result.gradle.releaseCandidateReleaseNotesUrl;
                printStream.printf(" -> %s", result.gradle.releaseCandidate.getVersion());
            }
            if (GradleReleaseChannel.NIGHTLY.getId().equals(this.gradleReleaseChannel) &&
                result.gradle.nightly.isUpdateAvailable() &&
                result.gradle.nightly.compareTo(result.gradle.current) > 0
            ) {
                updatePrinted = result.gradle.nightlyReleaseNotesUrl;
                printStream.printf(" -> %s", result.gradle.nightly.getVersion());
            }
            if (updatePrinted == null) {
                printStream.println(": UP-TO-DATE]");
            } else {
                printStream.printf("]%n     %s%n", updatePrinted);
            }
        }
    }
}
