package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import android.util.Log
import io.github.reugn.kotlin.backoff.StrategyBackoff
import io.github.reugn.kotlin.backoff.strategy.ExponentialStrategy
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.maxAge
import io.prometheus.client.Counter
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration

// Counters for monitoring the pushprox itself, compatible with the reference implementation in go.
private class Counters(enabled: Boolean) {
    private val enabled : Boolean
    private lateinit var scrapeErrorCounter : Counter
    private lateinit var pushErrorCounter : Counter
    private lateinit var pollErrorCounter : Counter
    private lateinit var pollSuccessCounter : Counter

    init {
        this.enabled = enabled
        if (enabled){
            // following 3 counters are compatible with reference implementation
            scrapeErrorCounter = Counter.build()
                .name("pushprox_client_scrape_errors_total")
                .help("Number of scrape errors")
                .register()
            pushErrorCounter = Counter.build()
                .name("pushprox_client_push_errors_total")
                .help("Number of push errors")
                .register()
            pollErrorCounter = Counter.build()
                .name("pushprox_client_poll_errors_total")
                .help("Number of poll errors")
                .register()

            // custom
            pollSuccessCounter = Counter.build()
                .name("pushprox_client_poll_total")
                .help("Number of succesfull polls")
                .register()
        }
    }

    fun scrapeError(){ if (enabled) scrapeErrorCounter.inc() }

    fun pushError(){ if (enabled) pushErrorCounter.inc() }

    fun pollError(){ if (enabled) pollErrorCounter.inc() }

    fun pollSuccess(){ if (enabled) pollSuccessCounter.inc() }
}

// Configuration object for this pushprox client
data class PushProxConfig(
    val myFqdn: String,
    val proxyURL: String,
    val retryInitialWaitSeconds: Int = 1, //TODO will this be even used?
    val retryMaxWaitSeconds: Int = 5, //TODO will this be even used?
    val enablePushProxClientMonitoring: Boolean = true,
)

// This is a stripped down kotlin implementation of github.com/prometheus-community/PushProx client
class PushProxClient(config: PushProxConfig) {
    private val config: PushProxConfig
    private val pollURL: String
    private val pushURL: String
    private lateinit var client: HttpClient
    private lateinit var counters : Counters
    private var running : Boolean = false

    init {
        this.config = config

        // make sure proxyURL ends with a single '/'
        val modifiedProxyURL = config.proxyURL.trim('/') + '/'
        log("ModifiedUrl", modifiedProxyURL)

        pollURL = "$modifiedProxyURL/poll"
        pushURL = "$modifiedProxyURL/push"
    }

    // initialize resource - heavier objects
    private fun setup(){
        // init counters if they are enabled
        counters = Counters(config.enablePushProxClientMonitoring)
        client = HttpClient(CIO)
    }

    // use this function to start exporting metrics to pushprox in the background
    public fun startBackground() {
        setup()
        loop(newBackoffFromFlags())
    }

    private suspend fun doPoll(){
        val response : HttpResponse = client.post(pollURL){
            setBody(config.myFqdn)
        }
        val responseBody: String = response.body<String>()
        log("responseBody in poll", responseBody)
        log("got scrape request", responseBody)

        //TODO asap

    }

    private fun parseRequest(request : String) : HttpRequestBuilder {
        var result : HttpRequestBuilder = HttpRequestBuilder()

        //TODO implement this

        return result
    }

    private fun doPush() {
        //TODO implement
    }

    private fun handleErr(){
        //TODO implement
    }

    private fun doScrape() {
        //TODO implement
    }

    private fun newBackoffFromFlags() : StrategyBackoff<Unit> {
        return StrategyBackoff<Unit>(
            strategy = ExponentialStrategy(
                expBase = 2,
                baseDelayMs = (config.retryInitialWaitSeconds * 1000).toLong(),
                maxDelayMs = (config.retryMaxWaitSeconds * 1000).toLong(),
            ),
        )
    }

    private suspend fun exceptionTest(){
        delay(1000L)
        throw IllegalArgumentException()
    }

    private fun loop(backoff: StrategyBackoff<Unit>) {
        // fire and forget a new coroutine
        GlobalScope.launch {
            launch {
                while (true) {
                    val job = launch {
                        log("pushprox loop now", "-")
                        var result = backoff.withRetries {
                            val result: Deferred<Unit> = async {
                                delay(1000L)
                            }

                            log("progress", "after poll")

                            // register poll error
                            try {
                                result.await()
                            } catch (e: Exception) {
                                log("progress", "catched")
                                log("exception", e.toString())
                                counters.pollError()
                                throw e
                            }
                        }

                        log("pushprox loop end", "end")
                    }
                    job.join()
                }
            }
        }
    }

    private fun log(title: String, text: String) {
        Log.v("PUSHPROXCLIENT", "$title: $text")
    }
}
