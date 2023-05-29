package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.compose

data class PromConfiguration(
    // the following are default values for various configuration settings
    val prometheusServerEnabled : Boolean = true,
    val prometheusServerPort : Int = 10101, //TODO register with prometheus foundation
    val pushproxEnabled : Boolean = false,
    val pushproxFqdn : String? = null,
    val pushproxProxyUrl : String? = null,
    val remoteWriteEnabled : Boolean = false,
    val remoteWriteScrapeInterval : Int = 30,
    val remoteWriteEndpoint : String? = null,
) {
    private val filepath : String = ""

    companion object {
        suspend fun configFileExists(): Boolean {
            //TODO implement this asap
            return false
        }

        suspend fun loadFromConfigFile(): PromConfiguration {
            //TODO open file, parse yaml, throw exception possibly
            return PromConfiguration()
        }
    }
}
