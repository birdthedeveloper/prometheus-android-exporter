package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.util.Log
import io.github.reugn.kotlin.backoff.StrategyBackoff
import io.github.reugn.kotlin.backoff.strategy.ExponentialStrategy
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import kotlinx.coroutines.CancellationException

/**
 * Counters for monitoring the pushprox itself, compatible with the reference
 * implementation in golang, source: https://github.dev/prometheus-community/PushProx
 */
private class Counters(collectorRegistry: CollectorRegistry) {
    private val collectorRegistry : CollectorRegistry
    private val scrapeErrorCounter : Counter
    private val pushErrorCounter : Counter
    private val pollErrorCounter : Counter

    init {
        this.collectorRegistry = collectorRegistry

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

    fun scrapeError(){ scrapeErrorCounter.inc() }

    fun pushError(){ pushErrorCounter.inc() }

    fun pollError(){ pollErrorCounter.inc() }
}

// Error in parsing HTTP header "Id" from HTTP request from Prometheus
class PushProxIdParseException(message: String) : Exception(message)

// Context object for pushprox internal functions to avoid global variables
data class PushProxContext(
    val client : HttpClient,
    val pollUrl : String,
    val pushUrl : String,
    val backoff : StrategyBackoff<Unit>,
    val fqdn : String,
)

// This is a stripped down kotlin implementation of github.com/prometheus-community/PushProx client
class PushProxClient(
    collectorRegistry: CollectorRegistry,
    private val performScrape: suspend () -> String
) {
    private val counters : Counters = Counters(collectorRegistry)
    private val retryInitialWaitSeconds : Int = 1
    private val retryMaxWaitSeconds : Int = 5

    // Use this function to start exporting metrics to pushprox in the background
    suspend fun startBackground(config: PushProxConfig) {
            val client : HttpClient = HttpClient() //TODO close this thing
            val context : PushProxContext = processConfig(client, config)
            loop(context)
    }

    private fun processConfig(client : HttpClient, config : PushProxConfig) : PushProxContext {
        var modifiedProxyURL = config.pushProxUrl.trim('/')

        if(
            !modifiedProxyURL.startsWith("http://") &&
            !modifiedProxyURL.startsWith("https://")
        ){
            modifiedProxyURL = "http://$modifiedProxyURL"
        }

        val pollURL : String = "$modifiedProxyURL/poll"
        val pushURL : String = "$modifiedProxyURL/push"

        return PushProxContext(
            client,
            pollURL,
            pushURL,
            newBackoffFromFlags(),
            config.pushProxFqdn,
        )
    }

    // Continuous poll from android phone to pushprox gateway
    private suspend fun doPoll(context : PushProxContext){
        log("poll", "polling now")
        val response : HttpResponse = context.client.post(context.pollUrl){
            setBody(context.fqdn)
        }
        log("here", "here")
        val responseBody: String = response.body<String>()
        doPush(context, responseBody)
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
        return result
    }

    // Parameter responseBody: response body of /poll request
    private suspend fun doPush(context : PushProxContext, pollResponseBody : String) {
        // perform scrape
        lateinit var scrapedMetrics : String
        try {
            scrapedMetrics = performScrape()
        }catch(e : Exception){
            counters.scrapeError()
            log("scrape exception", e.toString())
            return
        }

        try{
            val scrapeId : String = getIdFromResponseBody(pollResponseBody)
            val pushResponseBody: String = composeRequestBody(scrapedMetrics, scrapeId)

            val response : HttpResponse = context.client.request(context.pushUrl) {
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
                baseDelayMs = (retryInitialWaitSeconds * 1000).toLong(),
                maxDelayMs = (retryMaxWaitSeconds * 1000).toLong(),
            ),
        )
    }

    //TODO migrate to work manager
    private suspend fun loop(context : PushProxContext) {
        var shouldContinue : Boolean = true
        while (shouldContinue) {
            log("pushprox main loop", "loop start")
            // register poll error using try-catch block
            var result = context.backoff.withRetries {
                try {
                    doPoll(context)
                }catch(e : CancellationException){
                    shouldContinue = false
                }
                catch (e: Exception) {
                    for(exception in e.suppressed){
                        if(exception is CancellationException){
                            shouldContinue = false
                        }
                    }
                    log("exception encountered!", e.toString())
                    counters.pollError()
                    throw e
                }
            }
            log("pushprox main loop", "loop end")
        }
    }

    private fun log(title: String, text: String) {
        Log.v("PUSHPROXCLIENT", "$title: $text")
    }
}