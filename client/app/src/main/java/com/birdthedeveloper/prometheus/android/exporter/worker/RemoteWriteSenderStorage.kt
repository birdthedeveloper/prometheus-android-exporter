// Author: Martin Ptacek

package com.birdthedeveloper.prometheus.android.exporter.worker

import android.util.Log
import io.prometheus.client.Collector.MetricFamilySamples
import kotlinx.serialization.Serializable
import org.iq80.snappy.Snappy
import remote.write.RemoteWrite.Label
import remote.write.RemoteWrite.Sample
import remote.write.RemoteWrite.TimeSeries
import remote.write.RemoteWrite.WriteRequest
import java.util.Enumeration
import java.util.LinkedList
import java.util.Queue

private const val TAG: String = "REMOTE_WRITE_SENDER_STORAGE"

// This is a very primitive implementation, may require some optimization later
//
// No need for locks as all operations are run on a single thread, defined in PromWorker
// This class defines contract for RemoteWriteSender storage

// data classes, the same structure as MetricFamilySamples
data class MetricsScrape(
    val timeSeriesList : List<StorageTimeSeries>
){
    companion object {
        fun fromMfs(input : Enumeration<MetricFamilySamples>) : MetricsScrape {
            val timeSeriesList : MutableList<StorageTimeSeries> = mutableListOf()

            for (family in input){
                for (sample in family.samples){
                    if (sample != null){
                        val labels : MutableList<TimeSeriesLabel> = mutableListOf()

                        // name label
                        val sampleName : String = sample.name
                        val sampleNameLabel = TimeSeriesLabel(
                            name = "__name__",
                            value = sampleName
                        )
                        labels.add(sampleNameLabel)

                        // labels are stored in parallel lists -> iterate over two lists at a time
                        val labelNamesIterator = sample.labelNames.iterator()
                        val labelValuesIterator = sample.labelValues.iterator()

                        while (labelNamesIterator.hasNext() && labelValuesIterator.hasNext()) {
                            val labelName: String = labelNamesIterator.next()
                            val labelValue: String = labelValuesIterator.next()

                            val label = TimeSeriesLabel(
                                name = labelName,
                                value = labelValue,
                            )
                            labels.add(label)
                        }

                        val timeSeries = StorageTimeSeries(
                            labels = labels.toList(),
                            sample = TimeSeriesSample(
                                value = sample.value,
                                timeStampMs = System.currentTimeMillis(),
                            )
                        )
                        timeSeriesList.add(timeSeries)
                    }
                }
            }

            return MetricsScrape(
                timeSeriesList = timeSeriesList
            )
        }
    }
}

data class StorageTimeSeries(
    val sample : TimeSeriesSample,
    val labels : List<TimeSeriesLabel>,
)

@Serializable
data class TimeSeriesLabel(
    val name: String,
    val value: String,
) {
    fun toProtobufLabel(): Label {
        return Label.newBuilder()
            .setName(this.name)
            .setValue(this.value)
            .build()
    }
}

data class TimeSeriesSample(
    val timeStampMs: Long,
    val value: Double,
) {
    fun toProtobufSample(): Sample {
        return Sample.newBuilder()
            .setTimestamp(this.timeStampMs)
            .setValue(this.value)
            .build()
    }
}

abstract class RemoteWriteSenderStorage {
    companion object{
        const val maxMetricsAge : Int = 58 * 60 // 58 minutes

        val remoteWriteLabel: TimeSeriesLabel = TimeSeriesLabel(
            name = "backfill",
            value = "true",
        )
        fun encodeWithSnappy(data: ByteArray): ByteArray {
            return Snappy.compress(data)
        }
    }

    abstract fun writeScrapedSample(metricsScrape: MetricsScrape)
    abstract fun getScrapedSamplesCompressedProtobuf(howMany: Int): ByteArray
    abstract fun removeNumberOfScrapedSamples(number: Int)
    abstract fun isEmpty(): Boolean
    abstract fun getLength(): Int
}

