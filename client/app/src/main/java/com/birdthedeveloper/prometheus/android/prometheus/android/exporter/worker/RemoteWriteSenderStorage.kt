package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.util.Log
import com.google.protobuf.value
import io.prometheus.client.Collector
import io.prometheus.client.Collector.MetricFamilySamples
import org.xerial.snappy.Snappy
import remote.write.RemoteWrite.Label
import remote.write.RemoteWrite.Sample
import remote.write.RemoteWrite.TimeSeries
import remote.write.RemoteWrite.WriteRequest
import java.util.Enumeration
import java.util.LinkedList
import java.util.Queue

private const val TAG : String = "REMOTE_WRITE_SENDER_STORAGE"

// This is a very primitive implementation, may require some optimization later
//
// No need for locks as all operations are run on a single thread, defined in PromWorker
// This class defines contract for RemoteWriteSender storage

typealias MetricsScrape = Enumeration<Collector.MetricFamilySamples>

// HashMap<List of labels including name, List of TimeSeries samples to this TimeSeries>
private typealias ConverterHashMap = HashMap<List<TimeSeriesLabel>, MutableList<TimeSeriesSample>>

private data class TimeSeriesLabel(
    val name : String,
    val value : String,
){
    fun toProtobufLabel() : Label{
        return Label.newBuilder()
            .setName(this.name)
            .setValue(this.value)
            .build()
    }
}
private data class TimeSeriesSample(
    val timeStampMs : Long,
    val value : Double,
){
    fun toProtobufSample() : Sample{
        return Sample.newBuilder()
            .setTimestamp(this.timeStampMs)
            .setValue(this.value)
            .build()
    }
}

abstract class RemoteWriteSenderStorage {
    private val remoteWriteLabel : TimeSeriesLabel = TimeSeriesLabel(
        name = "backfill",
        value = "true",
    )
    protected fun encodeWithSnappy(data: ByteArray): ByteArray {
        return Snappy.compress(data)
    }

    private fun processLabels(sample : Collector.MetricFamilySamples.Sample,
                              familySampleName: String) : List<TimeSeriesLabel>{

        val result : MutableList<TimeSeriesLabel> = mutableListOf()

        // labels are stored in parallel lists -> iterate over two lists at a time
        val sampleLabelNamesIterator = sample.labelNames.iterator()
        val sampleLabelValuesIterator = sample.labelNames.iterator()

        while (sampleLabelNamesIterator.hasNext() && sampleLabelValuesIterator.hasNext()) {
            val labelName : String = sampleLabelNamesIterator.next()
            val labelValue : String = sampleLabelValuesIterator.next()

            val label : TimeSeriesLabel = TimeSeriesLabel(
                name = labelName,
                value = labelValue,
            )
            result.add(label)
        }

        // add name and remoteWrite mark
        val nameLabel = TimeSeriesLabel(name = "__name__", value = familySampleName)
        result.add(nameLabel)
        result.add(remoteWriteLabel)

        return result.toList()
    }

    private fun getTimeSeriesSample(sample : Collector.MetricFamilySamples.Sample) : TimeSeriesSample{
        return TimeSeriesSample(
            value = sample.value,
            timeStampMs = sample.timestampMs,
        )
    }

    private fun processTimeSeries(
        hashMap: ConverterHashMap, familySample : Collector.MetricFamilySamples){

        val familySampleName : String = familySample.name

        Log.v(TAG, "FamilySampleName: $familySampleName")

        for ( sample in familySample.samples ) {
            val labels : List<TimeSeriesLabel> = processLabels(sample, familySampleName)

            // TODO this may be useful in the future
            val sampleName : String = sample.name
            Log.v(TAG, "sampleName: $sampleName")

            val timeSeriesSample : TimeSeriesSample = getTimeSeriesSample(sample)

            if (hashMap[labels] == null){
                // this time series does not yet exist
                hashMap[labels] = mutableListOf(timeSeriesSample)
            }else{
                // this time series already exists
                hashMap[labels]!!.add(timeSeriesSample)
            }
        }
    }

    private fun hashMapEntryToProtobufTimeSeries(
        labels : List<TimeSeriesLabel>, samples : MutableList<TimeSeriesSample>) : TimeSeries {

        val timeSeriesBuilder : TimeSeries.Builder = TimeSeries.newBuilder()

        timeSeriesBuilder.addAllLabels(labels.map{
            it.toProtobufLabel()
        })

        timeSeriesBuilder.addAllSamples(samples.map{
            it.toProtobufSample()
        })

        return timeSeriesBuilder.build()
    }

    private fun hashmapToProtobufWriteRequest(hashMap: ConverterHashMap) : WriteRequest{
        val writeRequestBuilder : WriteRequest.Builder = WriteRequest.newBuilder()

        for (entry in hashMap){
            val timeSeries = hashMapEntryToProtobufTimeSeries(entry.key, entry.value)
            writeRequestBuilder.addTimeseries(timeSeries)
        }

        return writeRequestBuilder.build()
    }

    protected fun metricsScrapeListToProtobuf(input: List<MetricsScrape>) : WriteRequest {
        if(input.isEmpty()){
            throw Exception("Input is empty!")
        }

        val hashmap : ConverterHashMap = HashMap()

        for ( metricsScrape in input ){
            for ( familySample in metricsScrape ) {
                processTimeSeries(hashmap, familySample)
            }
        }

        return hashmapToProtobufWriteRequest(hashmap)
    }

    abstract fun writeScrapedSample(metricsScrape: MetricsScrape)
    abstract fun getScrapedSamplesCompressedProtobuf(howMany: Int): ByteArray
    abstract fun removeNumberOfScrapedSamples(number: Int)
    abstract fun isEmpty(): Boolean
    abstract fun getLength(): Int
}

class RemoteWriteSenderSimpleMemoryStorage : RemoteWriteSenderStorage() {
    private val data : Queue<MetricsScrape> = LinkedList()

    override fun getScrapedSamplesCompressedProtobuf(howMany: Int): ByteArray {
        if (howMany < 1){
            throw IllegalArgumentException("howMany must be bigger than zero")
        }

        val scrapedMetrics : MutableList<MetricsScrape> = mutableListOf()
        for (i in 1..howMany){
            val oneMetric : MetricsScrape? = data.poll()
            if(oneMetric == null){
                break
            }else{
                scrapedMetrics.add(oneMetric)
            }
        }

        val writeRequest : WriteRequest = this.metricsScrapeListToProtobuf(scrapedMetrics.toList())
        val bytes : ByteArray = writeRequest.toByteArray()
        return this.encodeWithSnappy(bytes)
    }

    override fun removeNumberOfScrapedSamples(number: Int) {
        if (number > 0){
            for (i in 1..number){
                data.remove()
            }
        }else{
            throw IllegalArgumentException("number must by higher than 0")
        }
    }

    override fun writeScrapedSample(metricsScrape: MetricsScrape) {
        data.add(metricsScrape)
    }

    override fun isEmpty(): Boolean {
        return data.isEmpty()
    }

    override fun getLength(): Int {
        return data.count()
    }
}

class RemoteWriteSenderDatabaseStorage : RemoteWriteSenderStorage() {
    override fun getScrapedSamplesCompressedProtobuf(howMany: Int): ByteArray {
        TODO("Not yet implemented")
    }

    override fun removeNumberOfScrapedSamples(number: Int) {
        TODO("Not yet implemented")
    }

    override fun writeScrapedSample(metricsScrape: MetricsScrape) {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getLength(): Int {
        TODO("Not yet implemented")
    }
}
