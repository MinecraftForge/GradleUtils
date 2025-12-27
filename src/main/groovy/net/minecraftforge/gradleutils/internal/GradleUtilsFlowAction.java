/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.internal;

import net.minecraftforge.gradleutils.shared.EnhancedFlowAction;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

abstract class GradleUtilsFlowAction<P extends GradleUtilsFlowAction.Parameters> extends EnhancedFlowAction<P> {
    static abstract class Parameters extends EnhancedFlowAction.EnhancedFlowParameters<GradleUtilsProblems> {
        Parameters() {
            super(GradleUtilsProblems.class);
        }
    }

    static abstract class JavadocLinksClassCheck extends GradleUtilsFlowAction<JavadocLinksClassCheck.Parameters> {
        static abstract class Parameters extends GradleUtilsFlowAction.Parameters {
            @Inject
            public Parameters() { }
        }

        @Inject
        public JavadocLinksClassCheck() { }

        @Override
        protected void run(Parameters parameters) {
            @Nullable var e = parameters.getFailure().getOrNull();
            if (e != null && contains(e, "io/freefair/gradle/plugins/maven/javadoc/JavadocLinkProvider"))
                parameters.problems().reportJavadocLinksNotOnClasspath(e);
        }
    }
}
