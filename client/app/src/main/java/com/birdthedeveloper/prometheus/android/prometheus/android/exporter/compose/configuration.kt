package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.compose

import android.content.Context
import androidx.work.Data
import androidx.work.workDataOf

private val defaultPrometheusServerPort : Int = 10101  //TODO register with prometheus foundation
private val defaultRemoteWriteScrapeInterval : Int = 30

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
    private val filepath : String = "config.yaml"
    private val alternativeFilepath : String = "config.yml"

    companion object {
        suspend fun configFileExists(context : Context): Boolean {
            //TODO implement this asap
            return false
        }

        fun fromWorkData(data : Data) : PromConfiguration{
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

        suspend fun loadFromConfigFile(): PromConfiguration {
            //TODO open file, parse yaml, throw exception possibly
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
