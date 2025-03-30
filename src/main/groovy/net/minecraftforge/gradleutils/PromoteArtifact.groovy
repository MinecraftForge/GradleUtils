/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.annotations.ApiStatus

import javax.inject.Inject
import javax.net.ssl.SSLContext
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * This task promotes an artifact on the Forge maven.
 *
 * @deprecated Aside from the fact that this is internal to Forge only due to the way the Files site works, this is planned to be superseded at some point in the future by a solution within the Files/Maven site itself, rather than through this task. While not scheduled for removal quite yet, don't count on it sticking around.
 */
@CompileStatic
@ApiStatus.Internal
@ApiStatus.Experimental
@Deprecated
@SuppressWarnings('GrDeprecatedAPIUsage')
abstract class PromoteArtifact extends DefaultTask {
    /** Registers a maven publication to be promoted to the Forge maven. */
    static TaskProvider<PromoteArtifact> register(Project project, MavenPublication publication, Type type) {
        project.tasks.register('promoteArtifact', PromoteArtifact) { task ->
            task.artifactGroup.set publication.groupId
            task.artifactName.set publication.artifactId
            task.artifactVersion.set publication.version
        }
    }

    @Inject abstract ProviderFactory getProviders();

    PromoteArtifact() {
        this.promotionType.convention Type.LATEST

        this.webhookURL.convention GradleUtils.getEnvVar('PROMOTE_ARTIFACT_WEBHOOK', this.providers)
        this.username.convention GradleUtils.getEnvVar('PROMOTE_ARTIFACT_USERNAME', this.providers)
        this.password.convention GradleUtils.getEnvVar('PROMOTE_ARTIFACT_PASSWORD', this.providers)

        this.onlyIf { this.webhookURL.present && this.username.present && this.password.present }
    }

    abstract @Input Property<String> getArtifactGroup()
    abstract @Input Property<String> getArtifactName()
    abstract @Input Property<String> getArtifactVersion()
    abstract @Input Property<Type> getPromotionType()
    abstract @Input Property<String> getWebhookURL()
    abstract @Input Property<String> getUsername()
    abstract @Input Property<String> getPassword()

    static enum Type {
        LATEST, RECOMMENDED;

        @Override
        String toString() {
            return super.toString().toLowerCase()
        }
    }

    @TaskAction
    void exec() {
        var client = HttpClient
            .newBuilder()
            .sslParameters(SSLContext.default.defaultSSLParameters.tap { protocols = ["TLSv1.3"] })
            .build()

        var request = HttpRequest
            .newBuilder()
            .uri(URI.create(this.webhookURL.get()))
            .setHeader("Content-Type", 'application/json')
            .setHeader("Authorization", "Basic ${Base64.getEncoder().encodeToString("${this.username.get()}:${this.password.get()}".getBytes())}")
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(new JsonBuilder(
                group: this.artifactGroup.get(),
                artifact: this.artifactName.get(),
                version: this.artifactVersion.get(),
                type: this.promotionType.getOrElse(Type.LATEST).toString()
            ).toPrettyString()))
            .build()

        var response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to promote artifact: ${response.statusCode()} ${response.body()}")
        }
    }
}
