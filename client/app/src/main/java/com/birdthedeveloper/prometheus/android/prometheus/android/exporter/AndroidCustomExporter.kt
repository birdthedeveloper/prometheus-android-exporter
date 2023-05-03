package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import android.util.Log
import io.prometheus.client.Collector
import io.prometheus.client.Collector.MetricFamilySamples
import io.prometheus.client.GaugeMetricFamily
import kotlinx.coroutines.runBlocking
import java.util.Arrays
import kotlinx.coroutines.*

private val TAG = "ANDROID_EXPORTER"

class AndroidCustomExporter(metricEngine: MetricsEngine) : Collector() {
    private val metricsEngineRef = metricEngine

    override fun collect(): List<MetricFamilySamples> {
        Log.v(TAG, "collecting")
        val mfs: MutableList<MetricFamilySamples> = ArrayList()

        // metrics definitions
        collectBatteryStatus(mfs)

        return mfs
    }

    fun collectBatteryStatus(mfs: MutableList<MetricFamilySamples>) {
        //TODO
        val batteryPercentageGauge = GaugeMetricFamily(
            "battery_percentage", //TODO convert to ratio
            "Current battery percentage", listOf(),
        )

        val batteryPercentage : Double = metricsEngineRef.getBatteryPercentage().toDouble()
        batteryPercentageGauge.addMetric(listOf(), batteryPercentage)
        mfs.add(batteryPercentageGauge)
    }
}
