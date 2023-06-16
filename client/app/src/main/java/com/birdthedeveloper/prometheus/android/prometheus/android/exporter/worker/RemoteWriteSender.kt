package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IndexOutOfBoundsException

private const val TAG: String = "REMOTE_WRITE_SENDER"

// This class stores information about scrapes to PROM_SERVER and PUSHPROX
// for purposes of scraping metrics on device and back-filling them later using remote write
//
// Only timestamps of successful scrapes are stored
internal class LastTimeRingBuffer(private val scrapeInterval: Int) {
    private val buffer: Array<Long> = Array(hysteresisMemory) { 0 }
    private var firstIndex: Int = -1

    companion object {
        private const val hysteresisMemory: Int = 3
        private const val hysteresisCoefficient : Double = 1.2
        private const val scrapeTimeCoefficient : Double = 2.2
    }

    fun setLastTime(timestamp: Long) {
        firstIndex = (++firstIndex) % hysteresisMemory
        buffer[firstIndex] = timestamp
    }

    fun getTimeByIndex(index: Int): Long {
        if (index > hysteresisMemory - 1) {
            throw IndexOutOfBoundsException("index cannot be bigger than hysteresisThreshold")
        }

        val bufferIndex: Int = (firstIndex - index)
        return if (bufferIndex < 0){
            buffer[hysteresisMemory + bufferIndex]
        }else{
            buffer[bufferIndex]
        }
    }

    fun checkScrapeDidNotHappenInTime(): Boolean {
        val now : Long = System.currentTimeMillis()
        return getTimeByIndex(0) < now - scrapeTimeCoefficient * scrapeInterval * 1000
    }

    fun checkScrapeDidNotHappenHysteresis(): Boolean {
        val diff = (hysteresisMemory * hysteresisCoefficient) * (scrapeInterval * 1000).toDouble()
        val scrapeOccurredAfterThis: Long = System.currentTimeMillis() - diff.toLong()

        // if any recorded time is lower: return  true
        for (i in 0 until hysteresisMemory) {
            if (getTimeByIndex(i) < scrapeOccurredAfterThis) {
                return true
            }
        }
        return false
    }
}

class TryExportMetricsAgainException(message : String) : Exception(message)

data class RemoteWriteConfiguration(
    val scrapeInterval: Int,
    val remoteWriteEndpoint: String,
    val collectorRegistry: CollectorRegistry,
    val maxSamplesPerExport: Int,
    val exportInterval: Int,
    val getContext: () -> Context,
)

class RemoteWriteSender(private val config: RemoteWriteConfiguration) {
    private val lastTimeRingBuffer = LastTimeRingBuffer(config.scrapeInterval)
    private val storage: RemoteWriteSenderStorage = RemoteWriteSenderSimpleMemoryStorage()
    private var scrapesAreBeingSent: Boolean = false
    private lateinit var client: HttpClient
    private var lastTimeRemoteWriteSent: Long = 0
    private var remoteWriteOn: Boolean = false

    private suspend fun performScrapeAndSaveIt(channel: Channel<Unit>) {
        Log.d(TAG, "performScrapeAndSaveIt start")

        val scrapedMetrics = config.collectorRegistry.metricFamilySamples()
        val metricsScrape : MetricsScrape = MetricsScrape.fromMfs(scrapedMetrics)

        storage.writeScrapedSample(metricsScrape)
        channel.send(Unit)

        Log.d(TAG, "performScrapeAndSaveIt end")
    }

    private fun insertInitialDummyScrape(){
        lastTimeRingBuffer.setLastTime(System.currentTimeMillis())
    }

    private suspend fun scraper(channel: Channel<Unit>) {
        val checkDelay : Long = 1000L

        insertInitialDummyScrape()

        while (true) {
            if (lastTimeRingBuffer.checkScrapeDidNotHappenInTime()) {
                remoteWriteOn = true
                Log.d(TAG, "Turning remote write on")

                performScrapeAndSaveIt(channel)
                delay(config.scrapeInterval * 1000L)

                while (lastTimeRingBuffer.checkScrapeDidNotHappenHysteresis()) {
                    Log.d(TAG, "Hysteresis loop start")
                    performScrapeAndSaveIt(channel)
                    delay(config.scrapeInterval * 1000L)
                    Log.d(TAG, "Hysteresis loop end")
                }

                Log.d(TAG, "Turning remote write off")
                remoteWriteOn = false
            }

            delay(checkDelay)
        }
    }

    private fun timeHasPassed(): Boolean {
        return lastTimeRemoteWriteSent < System.currentTimeMillis() - config.exportInterval * 1000
    }

    private fun conditionsForRemoteWrite(): Boolean {
        val ctx : Context = config.getContext()
        return Util.deviceIsConnectedToInternet(ctx) && (timeHasPassed() || enoughSamplesScraped())
    }

    private fun enoughSamplesScraped(): Boolean {
        return storage.getLength() > config.maxSamplesPerExport
    }

    private suspend fun exportToRemoteWriteEndpoint() {
        Log.d(TAG, "export To Remote Write Endpoint")
        if (!scrapesAreBeingSent) {
            scrapesAreBeingSent = true

            while (!storage.isEmpty()) {
                val body = storage.getScrapedSamplesCompressedProtobuf(config.maxSamplesPerExport)
                Log.d(TAG, "Exponential backoff to export remote write started")
                ExponentialBackoff.runWithBackoff({
                    sendRequestToRemoteWrite(body, config.maxSamplesPerExport)
                }, {
                   Log.d(TAG, "exportToRemoteWriteEndpointException, ${it.message}, ${it}, ${it.stackTraceToString()}")
                }, "Remote Write", false)
                Log.d(TAG, "Exponential backoff to export remote write finish")
            }
            lastTimeRemoteWriteSent = System.currentTimeMillis()

            scrapesAreBeingSent = false
        }
    }

    private suspend fun senderManager(channel: Channel<Unit>) {
        while (true) {
            Log.d(TAG, "Sender manager loop start")
            if (storage.isEmpty()) {
                // channel is conflated, one receive is enough
                // suspend here until sending remote write is needed
                Log.d(TAG, "Sender manager: waiting on channel receive")
                channel.receive()
                Log.d(TAG, "Sender Manager: channel received")
            }

            while (remoteWriteOn || !storage.isEmpty()) {
                if (conditionsForRemoteWrite()) {
                    exportToRemoteWriteEndpoint()
                }
                delay(1000)
            }
            Log.d(TAG, "Sender manager loop end")
        }
    }

    // entrypoint
    suspend fun start() {
        // conflated channel
        val channel = Channel<Unit>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        client = HttpClient()
        try {
            //TODO test this being coroutine scope
            coroutineScope { //TODO this could be a problem
                launch {
                    // check for outage in scrapes, save scrapes to storage
                    Log.d(TAG, "Launching scraper")
                    scraper(channel)
                }
                launch {
                    // send saved scrapes to remote write endpoint
                    Log.d(TAG, "Launching senderManager")
                    senderManager(channel)
                }
            }
        } finally {
            withContext(NonCancellable) {
                Log.d(TAG, "Canceling Remote Write Sender")
                channel.close()
                client.close()
            }
        }
    }

    fun countSuccessfulScrape() {
        lastTimeRingBuffer.setLastTime(System.currentTimeMillis())
    }

    private suspend fun sendRequestToRemoteWrite(body: ByteArray, numOfMetricScrapes : Int) {
        Log.d(TAG, "Exporting remote write to prometheus now")
        val response = client.post(config.remoteWriteEndpoint) {
            setBody(body)
            headers {
                append(HttpHeaders.ContentEncoding, "snappy")
                append(HttpHeaders.ContentType, "application/protobuf")
                append(HttpHeaders.UserAgent, "Prometheus Android Exporter")
                header("X-Prometheus-Remote-Write-Version", "0.1.0")
            }
        }

        Log.d(TAG, "Response status: ${response.status}")

        when (response.status) {
            HttpStatusCode.NoContent -> {
                // this export was successful
                storage.removeNumberOfScrapedSamples(numOfMetricScrapes)
            }
            HttpStatusCode.BadRequest -> {
                // probably some error or race condition has occured
                // give up trying to send this data
                storage.removeNumberOfScrapedSamples(numOfMetricScrapes)
            }
            else -> {
                throw TryExportMetricsAgainException("Status code: ${response.status.description}")
            }
        }
    }
}
