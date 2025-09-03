/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;

interface ToolsExtensionInternal extends ToolsExtension, HasPublicType {
    @Override
    default TypeOf<?> getPublicType() {
        return TypeOf.typeOf(ToolsExtension.class);
    }
}
