// Author: Martin Ptacek

package com.birdthedeveloper.prometheus.android.exporter.worker

import android.os.CpuUsageInfo
import android.util.Log
import io.prometheus.client.Collector
import io.prometheus.client.Gauge
import io.prometheus.client.GaugeMetricFamily

private const val TAG = "ANDROID_EXPORTER"

class AndroidCustomExporter(private val metricEngine: MetricsEngine) : Collector() {

    override fun collect(): List<MetricFamilySamples> {
        Log.d(TAG, "Collecting metrics now")
        val mfs: MutableList<MetricFamilySamples> = ArrayList()

        //TODO scrape_duration gauge
        val startTime = System.currentTimeMillis()

        // metrics definitions
        collectBatteryChargeRatio(mfs)
        collectUptimeInSeconds(mfs)
//        collectCpuUsage(mfs)
        collectHasWiFiConnection(mfs)
        collectHasCellularConnection(mfs)
//        collectDeviceTemperatures(mfs)
        collectAndroidInfo(mfs)

        collectScrapeDuration(mfs, startTime)

        Log.d(TAG, "Metrics collected")
        return mfs
    }

    private fun collectSteps(mfs: MutableList<MetricFamilySamples>){
        val gauge = GaugeMetricFamily(
            "steps",
            "Number of steps", listOf(),
        )

        gauge.addMetric(listOf(), 1.0)
        mfs.add(gauge)
    }

    private fun collectBatteryChargeRatio(mfs : MutableList<MetricFamilySamples>){
        val gauge = GaugeMetricFamily(
            "android_battery_charge_ratio",
            "Current battery charge",
            listOf(),
        )
        gauge.addMetric(listOf(), metricEngine.getBatteryChargeRatio())
        mfs.add(gauge)
    }

    private fun collectUptimeInSeconds(mfs : MutableList<MetricFamilySamples>){
        val gauge = GaugeMetricFamily(
            "android_uptime_seconds",
            "Android uptime in seconds",
            listOf(),
        )
        gauge.addMetric(listOf(), metricEngine.getUptimeInSeconds())
        mfs.add(gauge)
    }

    //TODO this does not work
    private fun collectCpuUsage(mfs : MutableList<MetricFamilySamples>){
        var coreIndex= 0
        val cpuUsage : Array<CpuUsageInfo> = metricEngine.getCpuUsage()
        val gaugeActive = GaugeMetricFamily(
            "android_cpu_active_seconds",
            "Active CPU time in seconds since last system booted",
            listOf("core"),
        )
        val gaugeTotal = GaugeMetricFamily(
            "android_cpu_total_seconds",
            "Total CPU time in seconds since last system booted",
            listOf("core")
        )

        cpuUsage.forEach {
            gaugeActive.addMetric(listOf((coreIndex++).toString()), it.active / 1000.0)
            gaugeActive.addMetric(listOf((coreIndex++).toString()), it.total / 1000.0)
        }

        mfs.addAll(listOf(gaugeTotal, gaugeActive))
    }

    //TODO does not work
    private fun collectDeviceTemperatures(mfs : MutableList<MetricFamilySamples>){
        val deviceTemperatures = metricEngine.getDeviceTemperatures()
        val gauge = GaugeMetricFamily(
            "android_system_temperature_celsius{where}` - ",
            "Temperature on the device",
            listOf("where")
        )
        deviceTemperatures.entries.forEach{
            gauge.addMetric(listOf(it.key), it.value)
        }
        mfs.add(gauge)
    }

    private fun collectHasWiFiConnection(mfs : MutableList<MetricFamilySamples>){

    }

    private fun collectHasCellularConnection(mfs : MutableList<MetricFamilySamples>){

    }

    private fun collectAndroidInfo(mfs : MutableList<MetricFamilySamples>){
        val gauge = GaugeMetricFamily(
            "android_system_info",
            "Static information about the android phone",
            listOf("manufacturer", "model", "os_release","cpu_core_count")
        )
        gauge.addMetric(listOf(
            metricEngine.getAndroidManufacturer(),
            metricEngine.getAndroidModel(),
            metricEngine.getAndroidOsVersion(),
            metricEngine.getNumberOfCpuCores().toString(),
        ), 1.0)
        mfs.add(gauge)
    }

    private fun collectNumberOfCpuCores(mfs : MutableList<MetricFamilySamples>){
        val gauge = GaugeMetricFamily(
            "android_system_info",
            "Static information about the android phone",
            listOf("manufacturer", "model", "os_release",)
        )
        gauge.addMetric(listOf(
            metricEngine.getAndroidManufacturer(),
            metricEngine.getAndroidModel(),
            metricEngine.getAndroidOsVersion(),
        ), 1.0)
        mfs.add(gauge)
    }

    private fun collectScrapeDuration(mfs : MutableList<MetricFamilySamples>, startTime : Long){
        val gauge = GaugeMetricFamily(
            "android_scrape_duration_seconds",
            "Duration of the metric scrape",
            listOf()
        )

        val differenceMilis = System.currentTimeMillis() - startTime
        gauge.addMetric(listOf(), differenceMilis / 1000.0)
        mfs.add(gauge)
    }
}
