/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyFactory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.jvm.toolchain.JavaToolchainService;

import javax.inject.Inject;
import java.util.concurrent.Callable;

abstract class ToolsExtensionImpl implements ToolsExtensionInternal {
    final NamedDomainObjectContainer<Tool.Definition> definitions;

    protected abstract @Inject Project getProject();

    protected abstract @Inject ObjectFactory getObjects();

    protected abstract @Inject DependencyFactory getDependencies();

    protected abstract @Inject JavaToolchainService getJavaToolchains();

    @Inject
    public ToolsExtensionImpl() {
        this.definitions = this.getObjects().domainObjectContainer(Tool.Definition.class, name -> getObjects().newInstance(ToolImpl.DefinitionImpl.class, name));
    }

    @Override
    public void configure(String name, Action<? super Tool.Definition> action) {
        this.definitions.register(name, action);
    }
}
