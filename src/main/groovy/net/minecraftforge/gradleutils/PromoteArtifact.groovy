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
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.annotations.ApiStatus

import javax.inject.Inject
import javax.net.ssl.SSLContext
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@CompileStatic
@ApiStatus.Experimental
abstract class PromoteArtifact extends DefaultTask {
    static TaskProvider<PromoteArtifact> register(Project project, MavenPublication publication) {
        project.tasks.register('promoteArtifact', PromoteArtifact, publication)
    }

    @Inject
    abstract ProviderFactory getProviders()

    @Inject
    PromoteArtifact(MavenPublication publication) {
        this.promotionType.convention Type.LATEST

        this.artifactGroup.set this.providers.provider { publication.groupId }
        this.artifactName.set this.providers.provider { publication.artifactId }
        this.artifactVersion.set this.providers.provider { publication.version }
        this.webhookURL.set GradleUtils.getEnvVar('PROMOTE_ARTIFACT_WEBHOOK', this.providers)
        this.username.set GradleUtils.getEnvVar('PROMOTE_ARTIFACT_USERNAME', this.providers)
        this.password.set GradleUtils.getEnvVar('PROMOTE_ARTIFACT_PASSWORD', this.providers)

        this.onlyIf { this.webhookURL.present && this.username.present && this.password.present }
    }

    @Input
    abstract Property<String> getArtifactGroup()

    @Input
    abstract Property<String> getArtifactName()

    @Input
    abstract Property<String> getArtifactVersion()

    @Input
    @Optional
    abstract Property<Type> getPromotionType()

    @Input
    abstract Property<String> getWebhookURL()

    @Input
    abstract Property<String> getUsername()

    @Input
    abstract Property<String> getPassword()

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
