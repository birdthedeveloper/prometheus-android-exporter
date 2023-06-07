package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

private const val TAG = "PUSHPROX_CLIENT"

// configuration class for pushprox
data class PushProxConfig(
    val pushProxUrl: String,
    val pushProxFqdn: String,
    val registry: CollectorRegistry,
    val performScrape: () -> String,
    val countSuccessfulScrape: suspend () -> Unit,
)

/**
 * Counters for monitoring the pushprox itself, compatible with the reference
 * implementation in golang, source: https://github.dev/prometheus-community/PushProx
 */
private class PushProxCounter(registry: CollectorRegistry) {
    private val pushErrorCounter: Counter = Counter.build()
        .name("pushprox_client_poll_errors_total")
        .help("Number of poll errors")
        .register(registry)
    private val scrapeErrorCounter: Counter = Counter.build()
        .name("pushprox_client_scrape_errors_total")
        .help("Number of scrape errors")
        .register(registry)

    private val pollErrorCounter: Counter = Counter.build()
        .name("pushprox_client_push_errors_total")
        .help("Number of push errors")
        .register(registry)

    fun scrapeError() {
        scrapeErrorCounter.inc()
    }

    fun pushError() {
        pushErrorCounter.inc()
    }

    fun pollError() {
        pollErrorCounter.inc()
    }
}

// Error in parsing HTTP header "Id" from HTTP request from Prometheus
class PushProxIdParseException(message: String) : Exception(message)

// Context object for pushprox internal functions to avoid global variables
data class PushProxContext(
    val client: HttpClient,
    val pollUrl: String,
    val pushUrl: String,
    val fqdn: String,
)

// This is a stripped down kotlin implementation of github.com/prometheus-community/PushProx client
class PushProxClient(private val pushProxConfig: PushProxConfig) {
    private val counters: PushProxCounter = PushProxCounter(pushProxConfig.registry)

    // Use this function to start exporting metrics to pushprox in the background
    suspend fun start() {
        Log.v(TAG, "Starting pushprox client")

        var client: HttpClient? = null
        try {
            client = HttpClient()
            val context: PushProxContext = getPushProxContext(client)
            loop(context)
        } finally {
            withContext(NonCancellable) {
                Log.v(TAG, "Canceling pushprox client")
                client?.close()
                Log.v(TAG, "PushProx http client canceled")
            }
        }
    }

    private fun getPushProxContext(client: HttpClient): PushProxContext {
        var modifiedProxyURL = pushProxConfig.pushProxUrl.trim('/')

        if (
            !modifiedProxyURL.startsWith("http://") &&
            !modifiedProxyURL.startsWith("https://")
        ) {
            modifiedProxyURL = "http://$modifiedProxyURL"
        }

        val pollURL: String = "$modifiedProxyURL/poll"
        val pushURL: String = "$modifiedProxyURL/push"

        return PushProxContext(
            client,
            pollURL,
            pushURL,
            pushProxConfig.pushProxFqdn,
        )
    }

    // Continuously poll from pushprox gateway
    private suspend fun doPoll(context: PushProxContext) {
        val response: HttpResponse = context.client.post(context.pollUrl) {
            setBody(context.fqdn)
        }
        val responseBody: String = response.body<String>()
        doPush(context, responseBody)
    }

    // get value of HTTP header "Id" from response body
    private fun getIdFromResponseBody(responseBody: String): String {

        val regexOptions = setOf<RegexOption>(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
        val match = Regex("^Id: (.*)", regexOptions).find(responseBody)
        match ?: throw PushProxIdParseException("Did not find header Id")

        val (id) = match.destructured
        return id
    }

    private fun composeRequestBody(scrapedMetrics: String, id: String): String {
        val httpHeaders = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain; version=0.0.4; charset=utf-8\r\n" +
                "Id: $id\r\n" +
                "X-Prometheus-Scrape-Timeout: 9.5\r\n"

        return httpHeaders + "\r\n" + scrapedMetrics
    }

    // Parameter responseBody: response body of /poll request
    private suspend fun doPush(context: PushProxContext, pollResponseBody: String) {
        // perform scrape
        val scrapedMetrics: String = try {
            pushProxConfig.performScrape()
        } catch (e: Exception) {
            Log.v(TAG, "Scrape exception ${e.toString()}")
            counters.scrapeError()
            ""
        }

        // push metrics to pushprox
        try {
            val scrapeId: String = getIdFromResponseBody(pollResponseBody)
            val pushRequestBody: String = composeRequestBody(scrapedMetrics, scrapeId)

            context.client.request(context.pushUrl) {
                method = HttpMethod.Post
                setBody(pushRequestBody)
            }

            pushProxConfig.countSuccessfulScrape()
        } catch (e: Exception) {
            counters.pushError()
            Log.v(TAG, "Push exception ${e.toString()}")
            return
        }
    }

    private suspend fun loop(context: PushProxContext) {
        while (true) {
            Log.v(TAG, "PushProxClient main loop start")

            ExponentialBackoff.runWithBackoff(
                function = { doPoll(context) },
                onException = { counters.pollError() }
            )

            Log.v(TAG, "PushProxClient main loop end")
        }
    }
}
