/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.internal;

import net.minecraftforge.gradleutils.PromotePublication;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.Internal;

interface PromotePublicationInternal extends PromotePublication, GradleUtilsTask, HasPublicType {
    @Override
    default @Internal TypeOf<?> getPublicType() {
        return TypeOf.typeOf(PromotePublication.class);
    }
}
