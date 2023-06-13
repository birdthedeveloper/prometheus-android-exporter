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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import remote.write.RemoteWrite
import remote.write.RemoteWrite.Label
import remote.write.RemoteWrite.TimeSeries
import remote.write.RemoteWrite.WriteRequest

private const val TAG: String = "REMOTE_WRITE_SENDER"

// This class stores information about scrapes to PROM_SERVER and PUSHPROX
// for purposes of scraping metrics on device and back-filling them later using remote write
//
// Only timestamps of succesfull scrapes are stored
private class LastTimeRingBuffer(private val scrapeIntervalMs: Int) {
    private val buffer : Array<Long> = Array(hysteresisThreshold) { _ -> 0 }
    private var firstIndex : Int = 0
    companion object{
        private const val hysteresisThreshold : Int = 3
    }

    fun setLastTime(timestamp : Long) {
        firstIndex = firstIndex++ % hysteresisThreshold
        buffer[firstIndex] = timestamp
    }

    private fun getTimeByIndex(index : Int) : Long {
        if(index > hysteresisThreshold - 1){
            throw IllegalArgumentException("index cannot be bigger than hysteresisThreshold")
        }

        val bufferIndex : Int = firstIndex + index % hysteresisThreshold
        return buffer[bufferIndex]
    }

    fun checkScrapeDidNotHappenInTime() : Boolean {
        return getTimeByIndex(0) < System.currentTimeMillis() - 3 * scrapeIntervalMs
    }

    fun checkScrapeDidNotHappenHysteresis() : Boolean {
        val scrapeOccurredAfterThis : Long = System.currentTimeMillis() - 5 * scrapeIntervalMs
        for (i in 0 until hysteresisThreshold) {
            if (getTimeByIndex(i) < scrapeOccurredAfterThis){
                return true
            }
        }
        return false
    }

}

data class RemoteWriteConfiguration(
    val scrape_interval: Int,
    val remote_write_endpoint: String,
    val collectorRegistry: CollectorRegistry,
    val getContext : () -> Context,
)

class RemoteWriteSender(private val config: RemoteWriteConfiguration) {
    private val lastTimeRingBuffer = LastTimeRingBuffer(config.scrape_interval * 1000)
    private val storage : RemoteWriteSenderStorage = RemoteWriteSenderSimpleMemoryStorage()
    private var scrapesAreBeingSent : Boolean = false
    private lateinit var client : HttpClient
    private var lastTimeRemoteWriteSent : Long = 0
    private var remoteWriteOn : Boolean = false

    companion object{
        private const val maxScrapesSentAtATime : Int = 500
        private const val delayBetweenSends : Int = 60 * 1000
    }

    private fun testGetRequestBody(): ByteArray {
        val label1: Label = Label.newBuilder()
            .setName("code")
            .setValue("200").build()

        val label2: Label = Label.newBuilder()
            .setName("handler")
            .setValue("/static/*filepath").build()

        val label3: Label = Label.newBuilder()
            .setName("instance")
            .setValue("localhost:9090").build()

        val label4: Label = Label.newBuilder()
            .setName("job")
            .setValue("prometheus").build()

        val label5: Label = Label.newBuilder()
            .setName("__name__")
            .setValue("prometheus_http_requests_total")
            .build()

        val specialLabel: Label = Label.newBuilder()
            .setName("prometheus_android_exporter")
            .setValue("remote_written")
            .build()

        val sample: RemoteWrite.Sample = RemoteWrite.Sample.newBuilder()
            .setValue(58.0)
            .setTimestamp(System.currentTimeMillis()).build()

        val timeSeries: TimeSeries = TimeSeries.newBuilder()
            .addAllLabels(listOf(label1, label2, label3, label4, label5, specialLabel))
            .addSamples(sample)
            .build()

        val request: WriteRequest = WriteRequest.newBuilder()
            .addTimeseries(timeSeries)
            .build()

        return request.toByteArray()
    }

    private fun performScrapeAndSaveIt() {
        val scrapedMetrics = config.collectorRegistry.metricFamilySamples()
        storage.writeScrapedSample(scrapedMetrics)
    }

    private suspend fun scraper(channel : Channel<Unit>){
        val checkDelay = 1000L
        while (true){
            if (lastTimeRingBuffer.checkScrapeDidNotHappenInTime()){
                remoteWriteOn = true
                performScrapeAndSaveIt()
                delay(config.scrape_interval * 1000L)


                while(lastTimeRingBuffer.checkScrapeDidNotHappenHysteresis()){
                    delay(config.scrape_interval * 1000L)
                    performScrapeAndSaveIt()
                }
            }
            delay(checkDelay)
        }
    }
    
    private suspend fun sendAll(){
        scrapesAreBeingSent = true
        while (!storage.isEmpty()){
            val body = storage.getScrapedSamplesCompressedProtobuf(maxScrapesSentAtATime)
            ExponentialBackoff.runWithBackoff( {sendRequestToRemoteWrite(body)}, {}, false,)
        }
        lastTimeRemoteWriteSent = System.currentTimeMillis()
    }

    private fun deviceHasInternet() : Boolean {
        val connectivityManager = config.getContext()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

        if (connectivityManager != null){
            val network = connectivityManager.getActiveNetworkCompat()
            val cap = connectivityManager.getNetworkCapabilities(network)
            if (cap != null && cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)){
                return true
            }
        }
        return false
    }

    private fun timeHasPassed() : Boolean {
        return lastTimeRemoteWriteSent < System.currentTimeMillis() - delayBetweenSends
    }

    private fun conditionsForRemoteWrite() : Boolean {
        return deviceHasInternet() && ( timeHasPassed() || enoughSamplesScraped() )
    }

    private fun enoughSamplesScraped() : Boolean {
        return storage.getLength() > maxScrapesSentAtATime
    }

    private suspend fun senderManager(channel : Channel<Unit>){
        if (!storage.isEmpty()){
            sendAll()
        }
        while (true) {
            if (storage.isEmpty()){
                channel.receive() // if storage is empty suspend here
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
    suspend fun start(){
       val channel = Channel<Unit>()
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
           withContext(NonCancellable){
               channel.close()
               client.close()
               Log.v(TAG, "Canceling Remote Write Sender")
           }
       }
    }

    fun countSuccessfulScrape(){
        Log.v(TAG, "Counting successful scrape")
        lastTimeRingBuffer.setLastTime(System.currentTimeMillis())
    }

    private suspend fun sendRequestToRemoteWrite(body : ByteArray){
        Log.v(TAG, "sending to prometheus remote write now")
        val response = client.post(config.remote_write_endpoint) {
            setBody(body)
            headers {
                append(HttpHeaders.ContentEncoding, "snappy")
                append(HttpHeaders.ContentType, "application/protobuf")
                append(HttpHeaders.UserAgent, "Prometheus Android Exporter")
                header("X-Prometheus-Remote-Write-Version", "0.1.0")
            }
        }

        Log.v(TAG, "Response status: ${response.status.toString()}")
        Log.v(TAG, "body: ${response.body<String>()}")
    }
}
