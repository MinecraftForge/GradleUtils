/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils;

import net.minecraftforge.gradleutils.shared.EnhancedPlugin;
import net.minecraftforge.gradleutils.shared.EnhancedTask;
import org.gradle.api.Project;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.Internal;

non-sealed interface PromotePublicationInternal extends PromotePublication, EnhancedTask, HasPublicType {
    @Override
    default Class<? extends EnhancedPlugin<? super Project>> pluginType() {
        return GradleUtilsPlugin.class;
    }

    @Override
    default @Internal TypeOf<?> getPublicType() {
        return TypeOf.typeOf(PromotePublication.class);
    }
}
