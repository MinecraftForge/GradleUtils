/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradleutils.internal

import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject
import javax.net.ssl.SSLContext
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@CompileStatic
@PackageScope abstract class PromotePublicationImpl extends DefaultTask implements PromotePublicationInternal {
    protected abstract @Inject ProviderFactory getProviders()

    @Inject
    PromotePublicationImpl(MavenPublication publication) {
        this.group = PublishingPlugin.PUBLISH_TASK_GROUP
        this.description = "Publishes Maven publication '${publication.name}' to the Forge Files site."

        this.artifactGroup.convention(this.providers.provider(this.project.&getGroup).map(Object.&toString)).set(this.providers.provider(publication.&getGroupId))
        this.artifactName.convention(this.project.extensions.getByType(BasePluginExtension).archivesName).set(this.providers.provider(publication.&getArtifactId))
        this.artifactVersion.convention(this.providers.provider(this.project.&getVersion).map(Object.&toString)).set(this.providers.provider(publication.&getVersion))

        this.promotionType.convention('latest')

        this.webhookURL.set(this.providers.environmentVariable('PROMOTE_ARTIFACT_WEBHOOK'))
        this.username.set(this.providers.environmentVariable('PROMOTE_ARTIFACT_USERNAME'))
        this.password.set(this.providers.environmentVariable('PROMOTE_ARTIFACT_PASSWORD'))

        this.onlyIf('If required info is missing, skip promotion') {
            final task = it as PromotePublicationImpl
            task.webhookURL.present && task.username.present && task.password.present
        }
    }

    @TaskAction
    void exec() {
        var client = HttpClient.newBuilder().tap {
            sslParameters(SSLContext.default.defaultSSLParameters.tap { protocols = ['TLSv1.3'] })
        }.build()

        var request = HttpRequest.newBuilder().tap {
            uri(URI.create(this.webhookURL.get()))
            setHeader('Content-Type', 'application/json')
            setHeader('Authorization', "Basic ${Base64.encoder.encodeToString("${this.username.get()}:${this.password.get()}".bytes)}")
            timeout(Duration.ofSeconds(10))
            POST(HttpRequest.BodyPublishers.ofString(new JsonBuilder(
                group: this.artifactGroup.get(),
                artifact: this.artifactName.get(),
                version: this.artifactVersion.get(),
                type: this.promotionType.get()
            ).toPrettyString()))
        }.build()

        var response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !== 200)
            throw new RuntimeException("Failed to promote artifact: ${response.statusCode()} ${response.body()}")
    }
}
