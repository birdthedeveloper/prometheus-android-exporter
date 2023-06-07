package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import java.sql.Timestamp
import java.util.LinkedList
import java.util.Queue

//TODO toto je na houby cele, musi byt structured misto byte array
class MetricsScrape(
    val compressedMetrics : ByteArray,
    val timestamp: Long,
)

// No need for locks as all operations are run on a single thread, defined in PromWorker
// This class defines contract for RemoteWriteSender storage
abstract class RemoteWriteSenderStorage {
    abstract fun writeScrapedSample(metricsScrape: MetricsScrape)
    abstract fun getNumberOfScrapedSamples(number: Int): List<MetricsScrape>
    abstract fun removeNumberOfScrapedSamples(number: Int)
    abstract fun isEmpty(): Boolean
    abstract fun getLength(): Int
}

class RemoteWriteSenderMemoryStorage : RemoteWriteSenderStorage() {
    // writeRequests are stored in protobuf already compressed
    private val data : Queue<MetricsScrape> = LinkedList<MetricsScrape>()

    override fun getNumberOfScrapedSamples(number: Int): List<MetricsScrape> {
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

class RemoteWriteSenterDatabaseStorage : RemoteWriteSenderStorage() {
    override fun getNumberOfScrapedSamples(number: Int): List<MetricsScrape> {
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