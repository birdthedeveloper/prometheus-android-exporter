package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import remote.write.RemoteWrite
import remote.write.RemoteWrite.Label
import remote.write.RemoteWrite.TimeSeries
import remote.write.RemoteWrite.WriteRequest

private const val TAG : String = "REMOTE_WRITE_SENDER"
data class RemoteWriteConfiguration(
    val scrape_interval : Int,
    val remote_write_endpoint : String,
    val performScrape : () -> String, //TODO this class needs it structured in objects
)

class RemoteWriteSender(private val config : RemoteWriteConfiguration) {

    //TODO implement this thing

    private fun getRequestBody() : ByteArray {
        val label : Label = Label.newBuilder()
            .setName("labelNameTest")
            .setValue("labelValueTest").build()
        val sample : RemoteWrite.Sample = RemoteWrite.Sample.newBuilder()
            .setValue(55.0)
            .setTimestamp(System.currentTimeMillis()).build()

        val timeSeries: TimeSeries = TimeSeries.newBuilder()
            .addLabels(label)
            .addSamples(sample)
            .build()

        val request : WriteRequest = WriteRequest.newBuilder()
            .addTimeseries(timeSeries)
            .build()

        return request.toByteArray()
    }

    private fun encdeWithSnappy(data : ByteArray) : ByteArray {
        // TODO implement this
        // github.com/xerial/snappy-java
        return data
    }

    suspend fun sendTestRequest(){
        val client = HttpClient()
        val response = client.post(config.remote_write_endpoint){
            setBody(getRequestBody())
            headers{
                append(HttpHeaders.ContentEncoding, "snappy")
                append(HttpHeaders.ContentType, "application/protobuf")
                append(HttpHeaders.UserAgent, "Prometheus Android Exporter")
                header("X-Prometheus-Remote-Write-Version", "0.1.0")
            }
        }

        Log.v(TAG, "Response status: ${response.status.toString()}")

        client.close()
    }
}

