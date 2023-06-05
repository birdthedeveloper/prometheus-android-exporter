package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

typealias MetricsScrape = String

// define the contract for Remote Write Sender storage
abstract class RemoteWriteSenderStorage {
    abstract fun writeScrapedSample(metricsScrape: MetricsScrape)
    abstract fun getNumberOfScrapedSamples(number: Int): List<MetricsScrape>
    abstract fun removeNumberOfScrapedSamples(number: Int)
    abstract fun isEmpty(): Boolean
}

class RemoteWriteSenderMemoryStorage : RemoteWriteSenderStorage() {
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
}