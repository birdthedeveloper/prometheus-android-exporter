// Author: Martin Ptacek

package com.birdthedeveloper.prometheus.android.exporter.compose

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.workDataOf
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

private const val TAG: String = "CONFIGURATION"

private const val defaultPrometheusServerPort: Int = 10101
private const val defaultRemoteWriteScrapeInterval: Int = 30 // seconds
private const val defaultRemoteWriteMaxSamplesPerExport: Int = 60 // seconds
private const val defaultRemoteWriteExportInterval: Int = 120 // seconds

// serialization classes for parsing YAML configuration file
@Serializable
data class PromConfigFile(
    val prometheus_server: PromServerConfigFile? = null,
    val pushprox: PushProxConfigFile? = null,
    val remote_write: RemoteWriteConfigFile? = null,
) {
    fun toPromConfiguration(): PromConfiguration {
        return PromConfiguration(
            pushproxProxyUrl = this.pushprox?.proxy_url ?: "",
            remoteWriteScrapeInterval = (this.remote_write?.scrape_interval
                ?: defaultRemoteWriteScrapeInterval).toString(),
            pushproxEnabled = this.pushprox?.enabled ?: false,
            pushproxFqdn = this.pushprox?.fqdn ?: "",
            remoteWriteEnabled = this.remote_write?.enabled ?: false,
            remoteWriteEndpoint = this.remote_write?.remote_write_endpoint ?: "",
            prometheusServerEnabled = this.prometheus_server?.enabled ?: true,
            prometheusServerPort = (this.prometheus_server?.port
                ?: defaultPrometheusServerPort).toString(),
            remoteWriteMaxSamplesPerExport = (this.remote_write?.max_samples_per_export
                ?: defaultRemoteWriteMaxSamplesPerExport).toString(),
            remoteWriteExportInterval = (this.remote_write?.export_interval
                ?: defaultRemoteWriteExportInterval).toString(),
        )
    }
}

@Serializable
data class PromServerConfigFile(
    val enabled: Boolean? = null,
    val port: Int? = null,
)

@Serializable
data class PushProxConfigFile(
    val enabled: Boolean? = null,
    val fqdn: String? = null,
    val proxy_url: String? = null,
)

@Serializable
data class RemoteWriteConfigFile(
    val enabled: Boolean? = null,
    val scrape_interval: Int? = null,
    val remote_write_endpoint: String? = null,
    val max_samples_per_export: Int? = null,
    val export_interval: Int? = null,
)

// configuration of a work manager worker
@Serializable
data class PromConfiguration(
    // the following are default values for various configuration settings
    val prometheusServerEnabled: Boolean = true,
    val prometheusServerPort: String = defaultPrometheusServerPort.toString(),
    val pushproxEnabled: Boolean = false,
    val pushproxFqdn: String = "",
    val pushproxProxyUrl: String = "",
    val remoteWriteEnabled: Boolean = false,
    val remoteWriteScrapeInterval: String = defaultRemoteWriteScrapeInterval.toString(),
    val remoteWriteEndpoint: String = "",
    val remoteWriteExportInterval: String = defaultRemoteWriteExportInterval.toString(),
    val remoteWriteMaxSamplesPerExport: String = defaultRemoteWriteMaxSamplesPerExport.toString(),
) {

    fun toStructuredText(): String {
        return """
            prometheus_server:
                enabled: $prometheusServerEnabled
                port: $prometheusServerPort
            pushprox:
                enabled: $pushproxEnabled
                fqdn: "$pushproxFqdn"
                proxy_url: "$pushproxProxyUrl"
            remote_write:
                enabled: $remoteWriteEnabled
                scrape_interval: $remoteWriteScrapeInterval
                export_interval: $remoteWriteExportInterval
                max_samples_per_export: $remoteWriteMaxSamplesPerExport
                remote_write_endpoint: "$remoteWriteEndpoint"
        """.trimIndent()
    }

    companion object {
        // data/user/0/com.birdthedeveloper.prometheus.android.exporter/files
        private const val filename: String = "config.yaml"
        private const val alternativeFilename: String = "config.yml"
        fun configFileExists(context: Context): Boolean {
            // using app-specific storage
            val file = File(context.filesDir, filename)
            val alternativeFile = File(context.filesDir, alternativeFilename)
            return file.exists() || alternativeFile.exists()
        }

        fun fromWorkData(data: Data): PromConfiguration {
            val jsonString: String = data.getString("json")
                ?: throw Exception("PromConfiguration serialization not working correctly!")
            return Json.decodeFromString(jsonString)
        }

        fun loadFromConfigFile(context: Context): PromConfiguration {
            Log.d(TAG, "Loading configuration file now")

            val file = File(context.filesDir, filename)
            val alternativeFile = File(context.filesDir, alternativeFilename)
            val fileContents: String = if (file.exists()) {
                file.readText()
            } else if (alternativeFile.exists()) {
                alternativeFile.readText()
            } else {
                throw Exception("configuration file does not exist!")
            }

            val parsedConfig: PromConfigFile = Yaml.default.decodeFromString(
                PromConfigFile.serializer(),
                fileContents
            )

            Log.d(TAG, "Configuration file loaded")

            return parsedConfig.toPromConfiguration()
        }
    }

    fun toWorkData(): Data {
        return workDataOf(
            "json" to Json.encodeToString(serializer(), this)
        )
    }
}
