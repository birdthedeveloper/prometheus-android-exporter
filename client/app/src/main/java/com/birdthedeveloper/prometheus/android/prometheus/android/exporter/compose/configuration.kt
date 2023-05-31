package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.compose

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.workDataOf
import kotlinx.serialization.Serializable
import java.io.File

private const val TAG : String = "CONFIGURATION"

private const val defaultPrometheusServerPort : Int = 10101  //TODO register within prometheus foundation
private const val defaultRemoteWriteScrapeInterval : Int = 30

@Serializable
data class PromConfigurationFile(
    val prometheusServerEnabled : Boolean = true,
    val prometheusServerPort : Int = defaultPrometheusServerPort,
    val pushproxEnabled : Boolean = false,
    val pushproxFqdn : String = "",
    val pushproxProxyUrl : String = "",
    val remoteWriteEnabled : Boolean = false,
    val remoteWriteScrapeInterval : Int = defaultRemoteWriteScrapeInterval,
    val remoteWriteEndpoint : String = "",
)

data class PromConfiguration(
    // the following are default values for various configuration settings
    val prometheusServerEnabled : Boolean = true,
    val prometheusServerPort : Int = defaultPrometheusServerPort,
    val pushproxEnabled : Boolean = false,
    val pushproxFqdn : String = "",
    val pushproxProxyUrl : String = "",
    val remoteWriteEnabled : Boolean = false,
    val remoteWriteScrapeInterval : Int = defaultRemoteWriteScrapeInterval,
    val remoteWriteEndpoint : String = "",
) {

    companion object {
        // data/user/0/com.birdthedeveloper.prometheus.android.prometheus.android.exporter/files
        private const val filename : String = "config.yaml"
        private const val alternativeFilename : String = "config.yml"
        suspend fun configFileExists(context : Context): Boolean {
            // using app-specific storage
            val file = File(context.filesDir, filename)
            val alternativeFile = File(context.filesDir, alternativeFilename)
            return file.exists() || alternativeFile.exists()
        }

        fun fromWorkData(data : Data) : PromConfiguration {
            return PromConfiguration(
                prometheusServerEnabled = data.getBoolean("0", true),
                prometheusServerPort = data.getInt("1", defaultPrometheusServerPort),
                pushproxEnabled = data.getBoolean("2", false),
                pushproxFqdn = data.getString("3") ?: "",
                pushproxProxyUrl = data.getString("4") ?: "",
                remoteWriteEnabled = data.getBoolean("5", false),
                remoteWriteScrapeInterval = data.getInt("6", defaultRemoteWriteScrapeInterval),
                remoteWriteEndpoint = data.getString("7") ?: "",
            )
        }

        suspend fun loadFromConfigFile(context : Context): PromConfiguration {
            Log.v(TAG, context.filesDir.absolutePath)

            val file = File(context.filesDir, filename)
            val alternativeFile = File(context.filesDir, alternativeFilename)
            val fileContents : String
            if (file.exists()){
                fileContents = file.readText()
            }else if (alternativeFile.exists()){
                fileContents = alternativeFile.readText()
            }else{
                throw Exception("configuration file does not exist!")
            }

            //TODO implement this asap

            return PromConfiguration()
        }
    }

    fun toWorkData() : Data{
        return workDataOf(
            "0" to this.prometheusServerEnabled,
            "1" to this.prometheusServerPort,
            "2" to this.pushproxEnabled,
            "3" to this.pushproxFqdn,
            "4" to this.pushproxProxyUrl,
            "5" to this.remoteWriteEnabled,
            "6" to this.remoteWriteScrapeInterval,
            "7" to this.remoteWriteEndpoint,
        )
    }
}
