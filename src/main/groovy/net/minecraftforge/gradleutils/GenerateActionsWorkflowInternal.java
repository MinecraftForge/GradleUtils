/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;

non-sealed interface GenerateActionsWorkflowInternal extends GenerateActionsWorkflow, GradleUtilsTask, HasPublicType {
    @Override
    default TypeOf<?> getPublicType() {
        return TypeOf.typeOf(GenerateActionsWorkflow.class);
    }
}
