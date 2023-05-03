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

// Configuration object for PrometheusServer class
data class PrometheusServerConfig(
    val port : Int,
    val performScrape : suspend () -> String,
)

// Expose metrics on given port using Ktor http server with CIO engine
class PrometheusServer(){

    fun startBackground(config : PrometheusServerConfig){
        //TODO dispose server

        val server = configureServer(config)
        GlobalScope.launch {
            launch{
                server.start(wait = true)
            }
        }

        log("startBackground", "done")
    }

    private suspend fun getMetrics(performScrape: suspend () -> String) : String{
        val result : String = try{
            performScrape()
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

    private fun configureServer(config : PrometheusServerConfig) : ApplicationEngine{
        return embeddedServer(CIO, port = config.port) {
            routing {
                get("/") {
                    call.respondText(getLandingPage())
                }
                get("/metrics") {
                    call.respondText(getMetrics(config.performScrape))
                }
            }
        }
    }


    private fun log(title: String, text: String) {
        Log.v("PROMETHEUS SERVER", "$title: $text")
    }
}
