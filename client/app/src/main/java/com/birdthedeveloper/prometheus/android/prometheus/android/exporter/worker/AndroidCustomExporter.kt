// Author: Martin Ptacek

package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.util.Log
import io.prometheus.client.Collector
import io.prometheus.client.GaugeMetricFamily

private const val TAG = "ANDROID_EXPORTER"

class AndroidCustomExporter(metricEngine: MetricsEngine) : Collector() {
    private val metricsEngineRef = metricEngine

    override fun collect(): List<MetricFamilySamples> {
        Log.d(TAG, "Collecting metrics now")
        val mfs: MutableList<MetricFamilySamples> = ArrayList()

        // metrics definitions
        collectBatteryStatus(mfs)

        Log.d(TAG, "Metrics collected")
        return mfs
    }

    private fun collectBatteryStatus(mfs: MutableList<MetricFamilySamples>) {
        //TODO
        val batteryPercentageGauge = GaugeMetricFamily(
            "battery_percentage", //TODO convert to ratio
            "Current battery percentage", listOf(),
        )

        val batteryPercentage: Double = metricsEngineRef.batteryChargeRatio().toDouble()
        batteryPercentageGauge.addMetric(listOf(), batteryPercentage)
        mfs.add(batteryPercentageGauge)
    }
}
