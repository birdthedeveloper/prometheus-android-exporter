package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import io.prometheus.client.Collector
import io.prometheus.client.Collector.MetricFamilySamples
import io.prometheus.client.GaugeMetricFamily
import kotlinx.coroutines.runBlocking
import java.util.Arrays
import kotlinx.coroutines.*

class AndroidCustomExporter(name: String) : Collector() {


//TODO pass context here, get baterry percentage from metrics engine
    override fun collect(): List<MetricFamilySamples> {
        val mfs: MutableList<MetricFamilySamples> = ArrayList()
        // With no labels.
        mfs.add(GaugeMetricFamily("my_gauge", "help", 42.0))
        // With labels
        val labeledGauge = GaugeMetricFamily("my_other_gauge", "help", Arrays.asList("labelname"))
        labeledGauge.addMetric(listOf("foo"), 4.0)
        labeledGauge.addMetric(listOf("bar"), 5.0)
        mfs.add(labeledGauge)
        println("Start blocking")
        return mfs
    }

    fun collectBaterryStatus(mfs: MutableList<MetricFamilySamples>) {
        //TODO
        val baterryPercentageGauge = GaugeMetricFamily(
            "baterry_percentage",
            "Baterry percentage", listOf(),
        )



        //baterryPercentageGauge.addMetric()


    }

}