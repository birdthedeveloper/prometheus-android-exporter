package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import android.util.Log
import io.ktor.server.application.call
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun something(){
    //TODO

}

// Configuration object for PrometheusServer class
data class PrometheusServerConfig(
    val port : Int = 8080,
    val performScrape : suspend () -> String,
)

// Expose metrics on given port using Ktor http server
class PrometheusServer(config: PrometheusServerConfig){
    private  val config : PrometheusServerConfig
    private lateinit var server : ApplicationEngine

    init {
        this.config = config
    }

    private suspend fun getMetrics() : String{
        val result : String = try{
            config.performScrape()
        }catch(e: Exception){
            ""
        }

        return result
    }

    private fun getLandingPage() : String{
        return """
            <html>
			<head><title>Android Exporter</title></head>
			<body>
			<h1>Android Exporter</h1>
			<p><a href="/metrics">Metrics</a></p>
			</body>
			</html>
        """.trimIndent()
    }

    private fun configureServer(){
        server = embeddedServer(CIO, port = config.port) {
            routing {
                get("/") {
                    call.respondText(getLandingPage())
                }
                get("/metrics") {
                    call.respondText(getMetrics())
                }
            }
        }
    }

    fun startBackground(){
        configureServer()

        GlobalScope.launch {
            launch{
                server.start(wait = true)
            }
        }
        log("startBackground", "done")
    }

    private fun log(title: String, text: String) {
        Log.v("PROMETHEUS SERVER", "$title: $text")
    }
}
