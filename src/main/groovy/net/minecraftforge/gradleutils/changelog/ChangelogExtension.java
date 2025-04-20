package net.minecraftforge.gradleutils.changelog;

import org.gradle.api.publish.maven.MavenPublication;
import org.jetbrains.annotations.Nullable;

public sealed interface ChangelogExtension permits ChangelogExtensionImpl {
    String NAME = "changelog";

    default void fromBase() {
        this.from(null);
    }

    void from(@Nullable String marker);

    boolean isGenerating();

    void publish(MavenPublication publication);

    boolean isPublishAll();

    void setPublishAll(boolean publishAll);
}
