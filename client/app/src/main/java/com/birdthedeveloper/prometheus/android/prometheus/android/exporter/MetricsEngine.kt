package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class MetricsEngine(context: Context) {
    private val contextRef = context
    public fun getBatteryPercentage() : Float{
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { intFilter ->
            contextRef.registerReceiver(null, intFilter)
        }
        //val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }

        batteryPct?: return -1.0f
        return batteryPct
    }
}
