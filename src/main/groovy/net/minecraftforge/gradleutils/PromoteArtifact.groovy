/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils

import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

import javax.inject.Inject
import javax.net.ssl.SSLContext
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@CompileStatic
@PackageScope abstract class PromoteArtifact extends DefaultTask {
    @PackageScope static TaskProvider<PromoteArtifact> register(Project project, MavenPublication publication, Type type) {
        project.tasks.register("promote${publication.name.capitalize()}", PromoteArtifact, publication, type)
    }

    protected abstract @Inject ProviderFactory getProviders();

    @Inject
    PromoteArtifact(MavenPublication publication, Type type) {
        this.webhookURL.convention this.providers.environmentVariable('PROMOTE_ARTIFACT_WEBHOOK')
        this.username.convention this.providers.environmentVariable('PROMOTE_ARTIFACT_USERNAME')
        this.password.convention this.providers.environmentVariable('PROMOTE_ARTIFACT_PASSWORD')

        this.artifactGroup.set publication.groupId
        this.artifactName.set publication.artifactId
        this.artifactVersion.set publication.version

        this.promotionType.convention(Type.LATEST).set type

        this.onlyIf { this.webhookURL.present && this.username.present && this.password.present }
    }

    abstract @Input Property<String> getArtifactGroup()
    abstract @Input Property<String> getArtifactName()
    abstract @Input Property<String> getArtifactVersion()
    abstract @Input Property<Type> getPromotionType()
    abstract @Input Property<String> getWebhookURL()
    abstract @Input Property<String> getUsername()
    abstract @Input Property<String> getPassword()

    @PackageScope static enum Type {
        LATEST, RECOMMENDED;

        @Override
        String toString() {
            return super.toString().toLowerCase()
        }

        static Type of(String type) {
            try {
                return PromoteArtifact.Type.valueOf(type.toUpperCase())
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid promotion type: $type. Known types: ${PromoteArtifact.Type.values()*.toString()}", e)
            }
        }
    }

    @TaskAction
    void exec() {
        var client = HttpClient
            .newBuilder()
            .sslParameters(SSLContext.default.defaultSSLParameters.tap { protocols = ['TLSv1.3'] })
            .build()

        var request = HttpRequest
            .newBuilder()
            .uri(URI.create(this.webhookURL.get()))
            .setHeader('Content-Type', 'application/json')
            .setHeader('Authorization', "Basic ${Base64.encoder.encodeToString "${this.username.get()}:${this.password.get()}".bytes}")
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
