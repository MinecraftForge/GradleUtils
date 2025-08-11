/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
/// GradleUtils is a convention plugin that is used by almost all of Forge's projects. It mostly consists of helper
/// classes and methods such as [net.minecraftforge.gradleutils.GradleUtilsExtension] and
/// [net.minecraftforge.gradleutils.PomUtils] that help eliminate duplicate code within buildscripts and allow us to
/// change any default values, such as Maven URLs, in case things change unexpectedly.
///
/// If you are a non-Forge consumer, it is recommended not to use any Forge-specific APIs such as the
/// [net.minecraftforge.gradleutils.GenerateActionsWorkflow] task.
package net.minecraftforge.gradleutils;
