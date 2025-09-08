/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.jvm.toolchain.JavaToolchainService;

import javax.inject.Inject;
import java.util.concurrent.Callable;

abstract class ToolsExtensionImpl implements ToolsExtensionInternal {
    final Callable<? extends JavaToolchainService> javaToolchains;
    final NamedDomainObjectContainer<? extends Tool.Definition> definitions;

    protected abstract @Inject ObjectFactory getObjects();

    @Inject
    public ToolsExtensionImpl(Callable<? extends JavaToolchainService> javaToolchains) {
        this.javaToolchains = javaToolchains;
        this.definitions = this.getObjects().domainObjectContainer(ToolImpl.DefinitionImpl.class);
    }

    @Override
    public void configure(String name, Action<? super Tool.Definition> action) {
        this.definitions.register(name, action);
    }
}
