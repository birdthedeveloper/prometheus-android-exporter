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
    val pushProxUrl : String,
    val pushProxFqdn : String,
    val registry : CollectorRegistry,
)

/**
 * Counters for monitoring the pushprox itself, compatible with the reference
 * implementation in golang, source: https://github.dev/prometheus-community/PushProx
 */
private class PushProxCounter(registry: CollectorRegistry) {
    private val pushErrorCounter : Counter = Counter.build()
            .name("pushprox_client_poll_errors_total")
            .help("Number of poll errors")
            .register(registry)
    private val scrapeErrorCounter : Counter = Counter.build()
            .name("pushprox_client_scrape_errors_total")
            .help("Number of scrape errors")
            .register(registry)

    private val pollErrorCounter : Counter = Counter.build()
            .name("pushprox_client_push_errors_total")
            .help("Number of push errors")
            .register(registry)

    fun scrapeError(){ scrapeErrorCounter.inc()}
    fun pushError(){ pushErrorCounter.inc() }
    fun pollError(){ pollErrorCounter.inc() } //TODO use this thing
}

// Error in parsing HTTP header "Id" from HTTP request from Prometheus //TODO wtf
class PushProxIdParseException(message: String) : Exception(message)

// Context object for pushprox internal functions to avoid global variables
data class PushProxContext(
    val client : HttpClient,
    val pollUrl : String,
    val pushUrl : String,
    val fqdn : String,
)

// This is a stripped down kotlin implementation of github.com/prometheus-community/PushProx client
class PushProxClient(config: PushProxConfig) {
    private val counters : PushProxCounter = PushProxCounter(config.registry)

    // Use this function to start exporting metrics to pushprox in the background
    suspend fun start(config: PushProxConfig) {
        Log.v(TAG, "Starting pushprox client")

        var client : HttpClient? = null
        try {
            client = HttpClient()
            val context : PushProxContext = getPushProxContext(client, config)
            loop(context)
        }finally {
            withContext(NonCancellable){
                Log.v(TAG, "Canceling pushprox client")
                client?.close()
            }
        }
    }


    private fun getPushProxContext(client : HttpClient, config : PushProxConfig) : PushProxContext {
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
            config.pushProxFqdn,
        )
    }

    //TODO refactor this function
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

    //TODO refactor this function
    // get value of HTTP header "Id" from response body
    private fun getIdFromResponseBody(responseBody: String) : String {

        val regexOptions = setOf<RegexOption>(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
        val match = Regex("^Id: (.*)", regexOptions).find(responseBody)
        match?: throw PushProxIdParseException("Did not find header Id")

        val (id) = match.destructured
        return id
    }

    //TODO refactor this function
    private fun composeRequestBody(scrapedMetrics: String, id : String) : String {
        val httpHeaders = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/plain; version=0.0.4; charset=utf-8\r\n" +
            "Id: $id\r\n" +
            "X-Prometheus-Scrape-Timeout: 9.5\r\n"

        return httpHeaders + "\r\n" + scrapedMetrics
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

    //TODO migrate to work manager
    private suspend fun loop(context : PushProxContext) {
        while (true) {
            Log.v(TAG, "PushProxClient main loop start")
            // register poll error using try-catch block
            //TODO backoff strategy
            //TODO asap
//            var result = context.backoff.withRetries {
//                try {
//                    doPoll(context)
//                }catch(e : CancellationException){
//                    shouldContinue = false
//                }
//                catch (e: Exception) {
//                    for(exception in e.suppressed){
//                        if(exception is CancellationException){
//                            shouldContinue = false
//                        }
//                    }
//                    log("exception encountered!", e.toString())
//                    counters.pollError()
//                    throw e
//                }
//            }
            Log.v(TAG,"PushProxClient main loop end")
        }
    }
}
