package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.iq80.snappy.Snappy
import remote.write.RemoteWrite
import remote.write.RemoteWrite.Label
import remote.write.RemoteWrite.TimeSeries
import remote.write.RemoteWrite.WriteRequest
import java.util.Timer
import kotlin.concurrent.schedule

private const val TAG: String = "REMOTE_WRITE_SENDER"

private class LastTimeMutex{
    private val mutex = Mutex()
    private var lastTime : Long = 0
    suspend fun setLastTime(time : Long){
        mutex.withLock {
            lastTime = time
        }
    }
    fun getLastTime() : Long { return lastTime }
}

private enum class RemoteWriteSenderState {
    REMOTE_WRITE,
    PUSHPROX_OR_PROMETHEUS_SERVER,
}

private class RemoteWriteSenderStateMutex {
    private val mutex = Mutex()
    private var remoteWriteSenderState = RemoteWriteSenderState.PUSHPROX_OR_PROMETHEUS_SERVER
    suspend fun setRemoteWriteSenderState(state : RemoteWriteSenderState){
        mutex.withLock {
            remoteWriteSenderState = state
        }
    }

    fun getRemoteWriteSenderState() : RemoteWriteSenderState {
        return remoteWriteSenderState
    }
}

data class RemoteWriteConfiguration(
    val scrape_interval: Int,
    val remote_write_endpoint: String,
    val performScrape: () -> String, //TODO this class needs it structured in objects
)

//TODO implement this thing
class RemoteWriteSender(private val config: RemoteWriteConfiguration) {
    private val lastTimeMutex = LastTimeMutex()

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

    suspend fun start(){
        runBlocking {
            //TODO from here
        }

    }

    fun countSuccessfulScrape(){
        Log.v(TAG, "countSuccesful scrape")
        //TODO implement this "last time" and mutex

        scheduleCheckScrapesHappened()
    }

    private fun encodeWithSnappy(data: ByteArray): ByteArray {
        return Snappy.compress(data)
    }

    suspend fun sendTestRequest() {
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

