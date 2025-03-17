package net.minecraftforge.gradleutils

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.Describable
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

@CompileStatic
@PackageScope
final class GradleUtilsSources {
    @CompileStatic
    static abstract class HasEnvVar implements ValueSource<Boolean, Parameters>, Describable {
        interface Parameters extends ValueSourceParameters {
            Property<String> getVariableName();
        }

        @Override
        Boolean obtain() {
            final env = System.getenv(this.parameters.variableName.get())
            env !== null && !env.isBlank() && !"false".equalsIgnoreCase(env)
        }

        @Override
        String getDisplayName() {
            "Environment Variable presense: ${this.parameters.variableName.get()}"
        }
    }

    private GradleUtilsSources() {}
}
