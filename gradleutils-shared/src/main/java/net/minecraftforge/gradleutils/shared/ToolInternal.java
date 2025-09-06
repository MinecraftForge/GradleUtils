/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;

interface ToolInternal extends Tool, HasPublicType {
    @Override
    default TypeOf<?> getPublicType() {
        return TypeOf.typeOf(Tool.class);
    }

    /// Gets this tool and returns a provider for the downloaded/cached file.
    ///
    /// @param cachesDir The caches directory to store the downloaded tool in
    /// @param toolsExt  The plugin's tools extension, which may contain overrides for the tool definition
    /// @return The provider to the tool file
    Tool.Resolved get(Provider<? extends Directory> cachesDir, ProviderFactory providers, ToolsExtensionImpl toolsExt);

    /// Gets this tool and returns a provider for the downloaded/cached file.
    ///
    /// @param cachesDir The caches directory to store the downloaded tool in
    /// @param toolsExt  The plugin's tools extension, which may contain overrides for the tool definition
    /// @return The provider to the tool file
    default Tool.Resolved get(Directory cachesDir, ProviderFactory providers, ToolsExtensionImpl toolsExt) {
        return this.get(providers.provider(() -> cachesDir), providers, toolsExt);
    }
}
