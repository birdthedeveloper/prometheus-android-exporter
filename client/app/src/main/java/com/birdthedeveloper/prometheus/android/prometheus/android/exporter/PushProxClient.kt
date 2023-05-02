package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import android.util.Log
import io.github.reugn.kotlin.backoff.StrategyBackoff
import io.github.reugn.kotlin.backoff.strategy.ExponentialStrategy
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// Counters for monitoring the pushprox itself, compatible with the reference
// implementation in golang, source: https://github.dev/prometheus-community/PushProx
private class Counters(collectorRegistry: CollectorRegistry?) {
    private val collectorRegistry : CollectorRegistry?
    private lateinit var scrapeErrorCounter : Counter
    private lateinit var pushErrorCounter : Counter
    private lateinit var pollErrorCounter : Counter
    private var enabled : Boolean = false

    init {
        this.collectorRegistry = collectorRegistry
        if (collectorRegistry != null){
            this.enabled = true

            // following 3 counters are compatible with reference implementation
            scrapeErrorCounter = Counter.build()
                .name("pushprox_client_scrape_errors_total")
                .help("Number of scrape errors")
                .register(collectorRegistry)
            pushErrorCounter = Counter.build()
                .name("pushprox_client_push_errors_total")
                .help("Number of push errors")
                .register(collectorRegistry)
            pollErrorCounter = Counter.build()
                .name("pushprox_client_poll_errors_total")
                .help("Number of poll errors")
                .register(collectorRegistry)
        }
    }

    fun scrapeError(){ if (enabled) scrapeErrorCounter.inc() }

    fun pushError(){ if (enabled) pushErrorCounter.inc() }

    fun pollError(){ if (enabled) pollErrorCounter.inc() }
}

// Configuration object for this pushprox client
data class PushProxConfig(
    val myFqdn: String,
    val proxyURL: String,
    val retryInitialWaitSeconds: Int = 1, //TODO will this be even used?
    val retryMaxWaitSeconds: Int = 5, //TODO will this be even used?
    val collectorRegistry: CollectorRegistry? = null,
    val performScrape: suspend () -> String,
)

// Error in parsing HTTP header "Id" from HTTP request from Prometheus
class PushProxIdParseException(message: String) : Exception(message)

// This is a stripped down kotlin implementation of github.com/prometheus-community/PushProx client
class PushProxClient(config: PushProxConfig) {

    //TODO dispose this thing - delete http client - something like the bellow stuff
    //val status = HttpClient().use { client ->
    //    // ...
    //}

    private val config: PushProxConfig
    private val pollURL: String
    private val pushURL: String
    private lateinit var client: HttpClient
    private lateinit var counters : Counters

    init {
        this.config = config

        // make sure proxyURL ends without '/'
        val modifiedProxyURL = config.proxyURL.trim('/')
        log("ModifiedUrl", modifiedProxyURL)

        pollURL = "$modifiedProxyURL/poll"
        pushURL = "$modifiedProxyURL/push"
    }

    // Initialize resources - heavier objects
    private fun setup(){
        // init counters if they are enabled
        counters = Counters(config.collectorRegistry)
        client = HttpClient(CIO)
    }

    // Use this function to start exporting metrics to pushprox in the background
    public fun startBackground() {
        setup()
        loop(newBackoffFromFlags())
    }

    // Continuous poll from android phone to pushprox gateway
    private suspend fun doPoll(){
        log("poll", "polling now")
        log(pollURL, pollURL)
        val response : HttpResponse = client.post(pollURL){
            setBody(config.myFqdn)
        }
        log("here", "here")
        val responseBody: String = response.body<String>()
        doPush(responseBody)
    }

    // get value of HTTP header "Id" from response body
    private fun getIdFromResponseBody(responseBody: String) : String {

        val regexOptions = setOf<RegexOption>(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
        val match = Regex("^Id: (.*)", regexOptions).find(responseBody)
        match?: throw PushProxIdParseException("Did not find header Id")

        val (id) = match.destructured
        return id
    }

    private fun composeRequestBody(scrapedMetrics: String, id : String) : String {
        val httpHeaders = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/plain; version=0.0.4; charset=utf-8\r\n" +
            "Id: $id\r\n" +
            "X-Prometheus-Scrape-Timeout: 9.5\r\n"

        val result : String = httpHeaders + "\r\n" + scrapedMetrics
        log("result", result)
        return result
    }

    // Parameter responseBody: response body of /poll request
    private suspend fun doPush(pollResponseBody : String) {
        // perform scrape
        lateinit var scrapedMetrics : String
        try {
            scrapedMetrics = config.performScrape()
        }catch(e : Exception){
            counters.scrapeError()
            log("scrape exception", e.toString())
            return
        }

        try{
            val scrapeId : String = getIdFromResponseBody(pollResponseBody)
            val pushResponseBody: String = composeRequestBody(scrapedMetrics, scrapeId)

            val response : HttpResponse = client.request(pushURL) {
                method = HttpMethod.Post
                setBody(pushResponseBody)
            }

        }catch(e : Exception){
            counters.pushError()
            log("push exception", e.toString())
            return
        }

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

    //TODO migrate to work manager
    private fun loop(backoff: StrategyBackoff<Unit>) {
        // fire and forget a new coroutine
        GlobalScope.launch {
            launch {
                while (true) {
                    val job = launch {
                        log("pushprox main loop", "loop start")
                        var result = backoff.withRetries {
                            // register poll error using try-catch block
                            try {
                                doPoll()
                            } catch (e: Exception) {
                                log("exception encountered!", e.toString())
                                counters.pollError()
                                throw e
                            }
                        }
                    }
                    job.join() // wait for the job to finish
                }
            }
        }
    }

    private fun log(title: String, text: String) {
        Log.v("PUSHPROXCLIENT", "$title: $text")
    }
}
