// Author: Martin Ptacek

package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.worker

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorManager
import android.os.BatteryManager
import androidx.core.content.ContextCompat.getSystemService

class MetricsEngine(private val context: Context) {
    private lateinit var sensorManager : SensorManager;
    init {
        //sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    public fun batteryChargeRatio(): Float {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { intFilter ->
            context.registerReceiver(null, intFilter)
        }
        //val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }

        batteryPct ?: return -1.0f
        return batteryPct
    }

    public fun batteryIsCharging(): Float {
        TODO("aa")
    }

    public fun somethingTodo(): Float {
        TODO("aa")
    }



//TODO
    ///TYPE_ACCELEROMETER	Yes	Yes	Yes	Yes
    //TYPE_AMBIENT_TEMPERATURE	Yes	n/a	n/a	n/a
    //TYPE_GRAVITY	Yes	Yes	n/a	n/a
    //TYPE_GYROSCOPE	Yes	Yes	n/a1	n/a1
    //TYPE_LIGHT	Yes	Yes	Yes	Yes
    //TYPE_LINEAR_ACCELERATION	Yes	Yes	n/a	n/a
    //TYPE_MAGNETIC_FIELD	Yes	Yes	Yes	Yes
    //TYPE_ORIENTATION	Yes2	Yes2	Yes2	Yes
    //TYPE_PRESSURE	Yes	Yes	n/a1	n/a1
    //TYPE_PROXIMITY	Yes	Yes	Yes	Yes
    //TYPE_RELATIVE_HUMIDITY	Yes	n/a	n/a	n/a
    //TYPE_ROTATION_VECTOR	Yes	Yes	n/a	n/a
    //TYPE_TEMPERATURE
//            - hardware metrics:
//    - basic hw sensors
//    - network availability
//    - 4G, 5G
//    - gps - glonass beidou ...
//    - battery, charging
//            node exporter metrics
//    - cpu
//    - ram
//    - scrape duration
//    - bluetooth - mac bluetooth
//    - nfc
//    - storage information
//    - system information - version .. device name, doba provozu
}
