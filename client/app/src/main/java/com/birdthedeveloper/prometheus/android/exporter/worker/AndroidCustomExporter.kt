// Author: Martin Ptacek

package com.birdthedeveloper.prometheus.android.exporter.worker

import android.util.Log
import io.prometheus.client.Collector
import io.prometheus.client.GaugeMetricFamily

private const val TAG = "ANDROID_EXPORTER"

class AndroidCustomExporter(private val metricEngine: MetricsEngine) : Collector() {

    override fun collect(): List<MetricFamilySamples> {
        Log.d(TAG, "Collecting metrics now")
        val mfs: MutableList<MetricFamilySamples> = ArrayList()

        //TODO scrape_duration gauge

        // metrics definitions
//        collectBatteryStatus(mfs)
//        collectGps(mfs)
        collectSteps(mfs)

        Log.d(TAG, "Metrics collected")
        return mfs
    }

//    private fun collectBatteryStatus(mfs: MutableList<MetricFamilySamples>) {
//        //TODO
//        val batteryPercentageGauge = GaugeMetricFamily(
//            "battery_percentage", //TODO convert to ratio
//            "Current battery percentage", listOf(),
//        )
//
//        val batteryPercentage: Double = metricsEngineRef.batteryChargeRatio().toDouble()
//        batteryPercentageGauge.addMetric(listOf(), batteryPercentage)
//        mfs.add(batteryPercentageGauge)
//    }

//    private fun collectGps(mfs: MutableList<MetricFamilySamples>){
//        //TODO
//    }

    private fun collectSteps(mfs: MutableList<MetricFamilySamples>){
        val gauge = GaugeMetricFamily(
            "steps",
            "Number of steps", listOf(),
        )

        gauge.addMetric(listOf(), 1.0)
        mfs.add(gauge)
    }
}
