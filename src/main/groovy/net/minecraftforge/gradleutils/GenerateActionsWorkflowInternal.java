package net.minecraftforge.gradleutils;

import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;

non-sealed interface GenerateActionsWorkflowInternal extends GenerateActionsWorkflow, HasPublicType {
    @Override
    default TypeOf<?> getPublicType() {
        return TypeOf.typeOf(GenerateActionsWorkflow.class);
    }
}
