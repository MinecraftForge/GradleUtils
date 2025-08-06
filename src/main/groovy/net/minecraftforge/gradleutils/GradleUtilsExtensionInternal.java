package net.minecraftforge.gradleutils;

import groovy.lang.Closure;
import net.minecraftforge.gradleutils.shared.Closures;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;

@SuppressWarnings("rawtypes") // public-facing closures
non-sealed interface GradleUtilsExtensionInternal extends GradleUtilsExtension, HasPublicType {
    @Override
    default TypeOf<?> getPublicType() {
        return TypeOf.typeOf(GradleUtilsExtension.class);
    }

    Closure forgeMaven = Closures.<MavenArtifactRepository>consumer(repo -> {
        repo.setName("MinecraftForge");
        repo.setUrl(Constants.FORGE_MAVEN);
    });

    Closure forgeReleaseMaven = Closures.<MavenArtifactRepository>consumer(repo -> {
        repo.setName("MinecraftForge releases");
        repo.setUrl(Constants.FORGE_MAVEN_RELEASE);
    });

    Closure minecraftLibsMaven = Closures.<MavenArtifactRepository>consumer(repo -> {
        repo.setName("Minecraft libraries");
        repo.setUrl(Constants.MC_LIBS_MAVEN);
    });

    non-sealed interface ForProject extends GradleUtilsExtensionInternal, GradleUtilsExtension.ForProject, HasPublicType {
        @Override
        default TypeOf<?> getPublicType() {
            return TypeOf.typeOf(GradleUtilsExtension.ForProject.class);
        }
    }
}
