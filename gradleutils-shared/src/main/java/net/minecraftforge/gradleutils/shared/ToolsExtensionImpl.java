/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;

import javax.inject.Inject;

abstract class ToolsExtensionImpl implements ToolsExtensionInternal {
    final NamedDomainObjectContainer<? extends Tool.Definition> definitions;

    protected abstract @Inject ObjectFactory getObjects();

    protected abstract @Inject ProviderFactory getProviders();

    protected abstract @Inject JavaToolchainService getJavaToolchains();

    @Inject
    public ToolsExtensionImpl() {
        this.definitions = this.getObjects().domainObjectContainer(DefinitionImpl.class);
    }

    @Override
    public void configure(String name, Action<? super Tool.Definition> action) {
        this.definitions.register(name, action);
    }

    static abstract class DefinitionImpl implements Tool.Definition, HasPublicType {
        private final String name;
        private final ConfigurableFileCollection classpath = this.getObjects().fileCollection();
        private final Property<String> mainClass = this.getObjects().property(String.class);
        private final Property<JavaLauncher> javaLauncher = this.getObjects().property(JavaLauncher.class);

        protected abstract @Inject ObjectFactory getObjects();

        @Inject
        public DefinitionImpl(String name) {
            this.name = name;
        }

        @Override
        public TypeOf<?> getPublicType() {
            return TypeOf.typeOf(Tool.Definition.class);
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public ConfigurableFileCollection getClasspath() {
            return this.classpath;
        }

        @Override
        public Property<String> getMainClass() {
            return this.mainClass;
        }

        @Override
        public Property<JavaLauncher> getJavaLauncher() {
            return this.javaLauncher;
        }
    }
}
