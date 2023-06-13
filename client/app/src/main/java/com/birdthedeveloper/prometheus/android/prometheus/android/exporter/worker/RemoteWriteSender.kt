package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.impl.utils.getActiveNetworkCompat
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val TAG: String = "REMOTE_WRITE_SENDER"

// This class stores information about scrapes to PROM_SERVER and PUSHPROX
// for purposes of scraping metrics on device and back-filling them later using remote write
//
// Only timestamps of succesfull scrapes are stored
private class LastTimeRingBuffer(private val scrapeIntervalMs: Int) {
    private val buffer: Array<Long> = Array(hysteresisThreshold) { 0 }
    private var firstIndex: Int = 0

    companion object {
        private const val hysteresisThreshold: Int = 3
    }

    fun setLastTime(timestamp: Long) {
        firstIndex = firstIndex++ % hysteresisThreshold
        buffer[firstIndex] = timestamp
    }

    private fun getTimeByIndex(index: Int): Long {
        if (index > hysteresisThreshold - 1) {
            throw IllegalArgumentException("index cannot be bigger than hysteresisThreshold")
        }

        val bufferIndex: Int = firstIndex + index % hysteresisThreshold
        return buffer[bufferIndex]
    }

    fun checkScrapeDidNotHappenInTime(): Boolean {
        return getTimeByIndex(0) < System.currentTimeMillis() - 3 * scrapeIntervalMs
    }

    fun checkScrapeDidNotHappenHysteresis(): Boolean {
        val scrapeOccurredAfterThis: Long = System.currentTimeMillis() - 5 * scrapeIntervalMs
        for (i in 0 until hysteresisThreshold) {
            if (getTimeByIndex(i) < scrapeOccurredAfterThis) {
                return true
            }
        }
        return false
    }

}

data class RemoteWriteConfiguration(
    val scrapeInterval: Int,
    val remoteWriteEndpoint: String,
    val collectorRegistry: CollectorRegistry,
    val maxSamplesPerExport: Int,
    val exportInterval: Int,
    val getContext: () -> Context,
)

class RemoteWriteSender(private val config: RemoteWriteConfiguration) {
    private val lastTimeRingBuffer = LastTimeRingBuffer(config.scrapeInterval * 1000)
    private val storage: RemoteWriteSenderStorage = RemoteWriteSenderSimpleMemoryStorage()
    private var scrapesAreBeingSent: Boolean = false
    private lateinit var client: HttpClient
    private var lastTimeRemoteWriteSent: Long = 0
    private var remoteWriteOn: Boolean = false

    private suspend fun performScrapeAndSaveIt(channel: Channel<Unit>) {
        Log.d(TAG, "performScrapeAndSaveIt start")
        val scrapedMetrics = config.collectorRegistry.metricFamilySamples()
        storage.writeScrapedSample(scrapedMetrics)
        channel.send(Unit)
        Log.d(TAG, "performScrapeAndSaveIt end")
    }

    private suspend fun scraper(channel: Channel<Unit>) {
        val checkDelay = 1000L
        while (true) {
            if (lastTimeRingBuffer.checkScrapeDidNotHappenInTime()) {
                remoteWriteOn = true
                performScrapeAndSaveIt(channel)
                delay(config.scrapeInterval * 1000L)

                while (lastTimeRingBuffer.checkScrapeDidNotHappenHysteresis()) {
                    delay(config.scrapeInterval * 1000L)
                    performScrapeAndSaveIt(channel)
                }
            }
            delay(checkDelay)
        }
    }

    private suspend fun sendAll() {
        Log.d(TAG, "sendAll")
        scrapesAreBeingSent = true
        while (!storage.isEmpty()) {
            val body = storage.getScrapedSamplesCompressedProtobuf(config.maxSamplesPerExport)
            ExponentialBackoff.runWithBackoff({ sendRequestToRemoteWrite(body) }, {}, false)
        }
        lastTimeRemoteWriteSent = System.currentTimeMillis()
    }

    private fun deviceHasInternet(): Boolean {
        val connectivityManager = config.getContext()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

        if (connectivityManager != null) {
            val network = connectivityManager.getActiveNetworkCompat()
            val cap = connectivityManager.getNetworkCapabilities(network)
            if (cap != null && cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                return true
            }
        }
        return false
    }

    private fun timeHasPassed(): Boolean {
        return lastTimeRemoteWriteSent < System.currentTimeMillis() - config.exportInterval * 1000
    }

    private fun conditionsForRemoteWrite(): Boolean {
        return deviceHasInternet() && (timeHasPassed() || enoughSamplesScraped())
    }

    private fun enoughSamplesScraped(): Boolean {
        return storage.getLength() > config.maxSamplesPerExport
    }

    private suspend fun senderManager(channel: Channel<Unit>) {
        while (true) {
            if (storage.isEmpty()) {
                // channel is conflated, one receive is enough
                // suspend here until sending remote write is needed
                Log.d(TAG, "Sender manager: waiting on channel receive")
                channel.receive()
                Log.d(TAG, "Sender Manager: channel received")
            }

            while (remoteWriteOn || !storage.isEmpty()) {
                if (conditionsForRemoteWrite()) {
                    sendAll()
                }
                delay(1000)
            }
        }
    }

    // entrypoint
    suspend fun start() {
        // conflated channel
        val channel = Channel<Unit>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        client = HttpClient()
        try {
            runBlocking {
                launch {
                    // check for outage in scrapes, save scrapes to storage
                    scraper(channel)
                }
                launch {
                    // send saved scrapes to remote write endpoint
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
        Log.d(TAG, "Counting successful scrape")
        lastTimeRingBuffer.setLastTime(System.currentTimeMillis())
    }

    private suspend fun sendRequestToRemoteWrite(body: ByteArray) {
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
    }
}
