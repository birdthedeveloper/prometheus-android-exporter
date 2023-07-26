// Author: Martin Ptacek

package com.birdthedeveloper.prometheus.android.exporter.worker

import android.content.Context
import android.hardware.Sensor
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorManager
import android.os.BatteryManager
import android.app.Activity
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log

private const val TAG = "METRICS_ENGINE"

class HwSensorsCache(
    var batteryChargeRatio : Double? = null,
    var numberOfSteps : Int? = null,
);

class MetricsEngine(private val context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val hwSensorsCache = HwSensorsCache()

    init {
        registerAllHwEventHandlers()
    }

    fun hwSensorsValues() : HwSensorsCache{
        return hwSensorsCache
    }

    private fun registerAllHwEventHandlers(){
        Log.d(TAG, "Registering all hw sensors")
        val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        sensor?.let{
            Log.d(TAG, "Sensor exists!")
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        //sensorManager.flush(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        Log.d(TAG, "Sensor Changed !!!!!!!!!!!!!!")
        if(event == null){
            return
        }
        if (event.values == null){
            return
        }

        when(event.sensor.type){
            Sensor.TYPE_PROXIMITY -> {
                hwSensorsCache.numberOfSteps = event.values[0].toInt()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed.")
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

//    public fun batteryChargeRatio(): Float {
//        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { intFilter ->
//            context.registerReceiver(null, intFilter)
//        }
//        //val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
//
//        val batteryPct: Float? = batteryStatus?.let { intent ->
//            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
//            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
//            level * 100 / scale.toFloat()
//        }
//
//        batteryPct ?: return -1.0f
//        return batteryPct
//    }
