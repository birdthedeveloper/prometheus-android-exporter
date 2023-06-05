package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import org.iq80.snappy.Snappy
import remote.write.RemoteWrite
import remote.write.RemoteWrite.Label
import remote.write.RemoteWrite.TimeSeries
import remote.write.RemoteWrite.WriteRequest

private const val TAG: String = "REMOTE_WRITE_SENDER"

data class RemoteWriteConfiguration(
    val scrape_interval: Int,
    val remote_write_endpoint: String,
    val performScrape: () -> String, //TODO this class needs it structured in objects
)

class RemoteWriteSender(private val config: RemoteWriteConfiguration) {

    //TODO implement this thing

    private fun getRequestBody(): ByteArray {
        val label: Label = Label.newBuilder()
            .setName("labelNameTest")
            .setValue("labelValueTest").build()

        val nameLabel: Label = Label.newBuilder()
            .setName("__name__")
            .setValue("testremotewritemetriccccccccvv")
            .build()

        val sample: RemoteWrite.Sample = RemoteWrite.Sample.newBuilder()
            .setValue(58.0)
            .setTimestamp(System.currentTimeMillis()).build()

        val timeSeries: TimeSeries = TimeSeries.newBuilder()
            .addLabels(label)
            .addLabels(nameLabel)
            .addSamples(sample)
            .build()

        val request: WriteRequest = WriteRequest.newBuilder()
            .addTimeseries(timeSeries)
            .build()

        return request.toByteArray()
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

