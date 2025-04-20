package net.minecraftforge.gradleutils.changelog;

import org.gradle.api.Action;
import org.gradle.api.Project;

interface Util {
    static void ensureAfterEvaluate(Project project, Action<? super Project> action) {
        if (project.getState().getExecuted())
            action.execute(project);
        else
            project.afterEvaluate(action);
    }
}
