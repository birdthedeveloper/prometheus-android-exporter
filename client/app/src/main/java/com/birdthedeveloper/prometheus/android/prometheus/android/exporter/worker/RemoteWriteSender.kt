package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.util.Log
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.iq80.snappy.Snappy
import remote.write.RemoteWrite
import remote.write.RemoteWrite.Label
import remote.write.RemoteWrite.TimeSeries
import remote.write.RemoteWrite.WriteRequest

private const val TAG: String = "REMOTE_WRITE_SENDER"

private enum class RemoteWriteSenderState {
    REMOTE_WRITE,
    PUSHPROX_OR_PROMETHEUS_SERVER,
}

private class LastTimeRingBuffer {
    //TODO implement this with ring array

    fun setLastTime(timestamp : Long) {
        //TODO implement this
    }

    private fun checkScrapeDidNotHappenInTime() : Boolean {
        return lastTimeMutex.getLastTime() < System.currentTimeMillis() - 3 * config.scrape_interval
    }

    private fun checkScrapeDidNotHappenHysteresis() : Boolean {
        return false //TODO implement this with ring buffer in lastTimeMutex
    }

}

data class RemoteWriteConfiguration(
    val scrape_interval: Int,
    val remote_write_endpoint: String,
    val collectorRegistry: CollectorRegistry,
)

class RemoteWriteSender(private val config: RemoteWriteConfiguration) {
    // TODO ring buffer for last time
    // TODO last time into it's own object with boolean functions
    // private val lastTimeMutex = LastTimeMutex()
    private var alreadyStoredSampleLength : Int = 0
    private val storage : RemoteWriteSenderStorage = RemoteWriteSenderMemoryStorage()
    private val scrapesAreBeingSentMutex = Mutex()

    private fun getRequestBody(): ByteArray {
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

    //TODO channel je bad
    //TODO v remotewriteseender storage musi byt mutex


    private suspend fun scraper(channel : Channel<Unit>){
        val checkDelay = 1000L
        while (true){
            if (checkScrapeDidNotHappenInTime()){
                delay(config.scrape_interval * 1000L)


                while(checkScrapeDidNotHappenHysteresis()){
                    delay(config.scrape_interval * 1000L)
                    performScrapeAndSaveIt
                }
            }
            delay(checkDelay)
        }
    }

    // sending metric scrapes to remote write endpoint will not be parallel
    private suspend fun sendAll(){
        scrapesAreBeingSentMutex.withLock {
            // Take all metric samples and send them in batches of (max_samples_per_send)
            // one by one batch


        }
    }

    private suspend fun senderManager(channel : Channel<Unit>){
        val alreadyStoredMetricScrapes : Int = storage.getLength()

        runBlocking {
            if (alreadyStoredMetricScrapes > 0){
                launch { // fire and forget
                    sendAll()
                }
            }

            channel.receive()


            // suspended on channel.receive

            // when there are enough to send:
            // start a sender

            // send with these conditions:
            //
        }

    }

    suspend fun start(){
       val channel = Channel<Unit>()
       try {
           runBlocking {
               launch {
                   scraper(channel)
               }
               launch {
                   senderManager(channel)
               }
           }
       } finally {
           withContext(NonCancellable){
               channel.close()
               Log.v(TAG, "Canceling Remote Write Sender")
           }
       }
    }

    suspend fun countSuccessfulScrape(){
        Log.v(TAG, "Counting successful scrape")
        lastTimeMutex.setLastTime(System.currentTimeMillis())
    }

    private suspend fun sendTestRequest() {
        Log.v(TAG, "sending to prometheus now")
        val client = HttpClient()
        val response = client.post(config.remote_write_endpoint) {
            setBody(encodeWithSnappy(getRequestBody()))
            headers {
                append(HttpHeaders.ContentEncoding, "snappy")
                append(HttpHeaders.ContentType, "application/protobuf")
                append(HttpHeaders.UserAgent, "Prometheus Android Exporter")
                header("X-Prometheus-Remote-Write-Version", "0.1.0")
            }
        }

        Log.v(TAG, "Response status: ${response.status.toString()}")
        Log.v(TAG, "body: ${response.body<String>()}")

        client.close()
    }
}
