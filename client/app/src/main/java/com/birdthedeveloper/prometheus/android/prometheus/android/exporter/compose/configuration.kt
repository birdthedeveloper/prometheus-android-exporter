package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.compose

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.workDataOf
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

private const val TAG: String = "CONFIGURATION"

private const val defaultPrometheusServerPort: Int =
    10101  //TODO register within prometheus foundation
private const val defaultRemoteWriteScrapeInterval: Int = 30

// serialization classes for parsing YAML configuration file
@Serializable
data class PromConfigFile(
    val prometheus_server: PromServerConfigFile?,
    val pushprox: PushProxConfigFile?,
    val remote_write: RemoteWriteConfigFile?,
) {
    fun toPromConfiguration(): PromConfiguration {
        return PromConfiguration(
            pushproxProxyUrl = this.pushprox?.proxy_url ?: "",
            remoteWriteScrapeInterval = this.remote_write?.scrape_interval
                ?: defaultRemoteWriteScrapeInterval,
            pushproxEnabled = this.pushprox?.enabled ?: false,
            pushproxFqdn = this.pushprox?.fqdn ?: "",
            remoteWriteEnabled = this.remote_write?.enabled ?: false,
            remoteWriteEndpoint = this.remote_write?.remote_write_endpoint ?: "",
            prometheusServerEnabled = this.prometheus_server?.enabled ?: true,
            prometheusServerPort = this.prometheus_server?.port ?: defaultPrometheusServerPort,
        )
    }
}

@Serializable
data class PromServerConfigFile(
    val enabled: Boolean?,
    val port: Int?,
)

@Serializable
data class PushProxConfigFile(
    val enabled: Boolean?,
    val fqdn: String?,
    val proxy_url: String?
)

@Serializable
data class RemoteWriteConfigFile(
    val enabled: Boolean?,
    val scrape_interval: Int?,
    val remote_write_endpoint: String?,
)

// configuration of a work manager worker
@Serializable
data class PromConfiguration(
    // the following are default values for various configuration settings
    val prometheusServerEnabled: Boolean = true,
    val prometheusServerPort: Int = defaultPrometheusServerPort,
    val pushproxEnabled: Boolean = false,
    val pushproxFqdn: String = "",
    val pushproxProxyUrl: String = "",
    val remoteWriteEnabled: Boolean = false,
    val remoteWriteScrapeInterval: Int = defaultRemoteWriteScrapeInterval,
    val remoteWriteEndpoint: String = "",
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
                remote_write_endpoint: "$remoteWriteEndpoint"
        """.trimIndent()
    }

    companion object {
        // data/user/0/com.birdthedeveloper.prometheus.android.prometheus.android.exporter/files
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
            return Json.decodeFromString<PromConfiguration>(jsonString)
        }

        fun loadFromConfigFile(context: Context): PromConfiguration {
            Log.v(TAG, context.filesDir.absolutePath)

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

            Log.v(TAG, parsedConfig.prometheus_server?.port.toString())

            return parsedConfig.toPromConfiguration()
        }
    }

    fun toWorkData(): Data {
        return workDataOf(
            "json" to Json.encodeToString(serializer(), this)
        )
    }
}
