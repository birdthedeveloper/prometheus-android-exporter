package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.util.Log
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

private const val TAG = "PROMETHEUS_SERVER"

// Configuration object for PrometheusServer
data class PrometheusServerConfig(
    val port : Int,
    val performScrape : suspend () -> String,
)


// Expose metrics on given port using Ktor http server
class PrometheusServer() {
    companion object {
        suspend fun start(config: PrometheusServerConfig) {
            Log.v(TAG, "Starting prometheus server")

            val server = configureServer(config)

            try{
                server.start()
                delay(Long.MAX_VALUE)
            }finally {
                withContext(NonCancellable){
                    Log.v(TAG, "3")
                    server.stop()
                    Log.v(TAG, "4")
                }
            }
        }

        private fun getLandingPage(): String {
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

        private fun notFoundPage(): String {
            return """
                <html>
                <head><title>Not found</title></head>
                <body>
                <h1>Not found</h1>
                <p><a href="/metrics">Metrics</a></p>
                </body>
                </html>
            """.trimIndent()
        }

        private fun configureServer(config: PrometheusServerConfig): ApplicationEngine {
            //TODO testing of different providers (CIO, netty, jetty ... for performance)
            return embeddedServer(CIO, port = config.port) {
                install(StatusPages) {
                    status(HttpStatusCode.NotFound) { call, status ->
                        call.respondText(text = notFoundPage(), status = status)
                    }
                    exception<Throwable> { call, _ ->
                        call.respondText(
                            text = "Server error" ,
                            status = HttpStatusCode.InternalServerError,
                        )
                    }
                }
                routing {
                    get("/") {
                        call.respondText(getLandingPage())
                    }
                    get("/metrics") {
                        call.respondText(config.performScrape())
                    }
                }
            }
        }
    }
}
