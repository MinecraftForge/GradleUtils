/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.shared;

import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.gradle.api.flow.FlowAction;
import org.gradle.api.flow.FlowParameters;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

/// The enhanced flow action adds convenience methods to the standard flow action provided by Gradle.
///
/// @param <P> The type of enhanced flow parameters to use
/// @implNote Consumers *must* set the [EnhancedFlowParameters#getFailure()] property in order for it to work properly.
/// The build failure is provided by [org.gradle.api.flow.FlowProviders#getBuildWorkResult()] mapped by
/// [org.gradle.api.flow.BuildWorkResult#getFailure()].
public abstract class EnhancedFlowAction<P extends EnhancedFlowAction.EnhancedFlowParameters<?>> implements FlowAction<P> {
    /// The parameters, including the [#getFailure()] property.
    public static abstract class EnhancedFlowParameters<P extends EnhancedProblems> implements FlowParameters {
        private final Class<P> problemsType;
        private transient @Nullable P problems;

        private final Property<Throwable> failure;

        protected abstract @Inject ObjectFactory getObjects();

        protected EnhancedFlowParameters(Class<P> problemsType) {
            this.problemsType = problemsType;

            this.failure = this.getObjects().property(Throwable.class);
        }

        public P problems() {
            return this.problems == null ? this.problems = this.getObjects().newInstance(this.problemsType) : this.problems;
        }

        public @Optional @Input Property<Throwable> getFailure() {
            return this.failure;
        }
    }

    protected EnhancedFlowAction() { }

    /// Checks if a given throwable's [message][Throwable#getMessage()] contains the given string
    /// ({@linkplain StringGroovyMethods#containsIgnoreCase(CharSequence, CharSequence) ignoring case}). This check
    /// includes checking all the throwable's [causes][Throwable#getCause()].
    ///
    /// @param e The throwable to check
    /// @param s The string to match for (ignoring case)
    /// @return If the throwable's message contains the string
    protected static boolean contains(Throwable e, String s) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (StringGroovyMethods.containsIgnoreCase(cause.getMessage(), s))
                return true;
        }

        return false;
    }

    /// Executes this flow action.
    ///
    /// @param parameters The parameters for this action
    /// @implNote This method is final to avoid Gradle eating any exceptions thrown by it. Consumers should instead
    /// implement [#run(EnhancedFlowParameters)].
    @Override
    public final void execute(P parameters) {
        try {
            this.run(parameters);
        } catch (Exception e) {
            // wrapping in this exception will prevent gradle from eating the stacktrace
            throw new RuntimeException(e);
        }
    }

    /// Runs this flow action. Used by [#execute(EnhancedFlowParameters)].
    ///
    /// @param parameters The parameters for this action
    /// @throws Exception If any error occurs
    protected abstract void run(P parameters) throws Exception;
}
