/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
/**
 * This package contains common code that is shared between MinecraftForge's Gradle plugins. The purpose is to reduce
 * duplicate code and keep the majority of complex implementation details here, instead of in the implementing plugins.
 * <p>The majority of these implementations consist of "enhanced" types of existing Gradle types, which are extensions
 * that include Forge-specific helper methods.</p>
 **/
@ApiStatus.Internal
@NotNullByDefault
package net.minecraftforge.gradleutils.shared;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNullByDefault;
