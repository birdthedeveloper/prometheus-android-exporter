package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import io.prometheus.client.Collector
import io.prometheus.client.Collector.MetricFamilySamples
import io.prometheus.client.GaugeMetricFamily
import kotlinx.coroutines.runBlocking
import java.util.Arrays
import kotlinx.coroutines.*

class AndroidCustomExporter(metricEngine: MetricsEngine) : Collector() {
    private val metricsEngineRef = metricEngine

    override fun collect(): List<MetricFamilySamples> {
        val mfs: MutableList<MetricFamilySamples> = ArrayList()

        // metrics definitions
        collectBatteryStatus(mfs)

        return mfs
    }

    fun collectBatteryStatus(mfs: MutableList<MetricFamilySamples>) {
        //TODO
        val batteryPercentageGauge = GaugeMetricFamily(
            "battery_percentage",
            "Current battery percentage", listOf(),
        )

        val batteryPercentage : Double = metricsEngineRef.getBatteryPercentage().toDouble()
        batteryPercentageGauge.addMetric(listOf(), batteryPercentage)
        mfs.add(batteryPercentageGauge)
    }
}
