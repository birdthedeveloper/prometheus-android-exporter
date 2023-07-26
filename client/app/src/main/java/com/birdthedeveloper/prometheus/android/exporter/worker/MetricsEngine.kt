// Author: Martin Ptacek

package com.birdthedeveloper.prometheus.android.exporter.worker

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.os.BatteryManager
import android.os.Build
import android.os.CpuUsageInfo
import android.os.HardwarePropertiesManager
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.util.Log

private const val TAG = "METRICS_ENGINE"

data class AxisSpecificGauge(
    val x : Double,
    val y : Double,
    val z : Double,
)

class HwSensorsCache(
    var batteryChargeRatio : Double? = null,
    var numberOfSteps : Int? = null,


    var headingDegrees : Double? = null,
    var headingAccuracyDegrees : Double? = null,
    var hingeAngleDegrees : Double? = null,
    var offbodyDetect : Double? = null,
    var ambientTemperatureCelsius : Double? = null,
    var relativeHumidityPercent : Double? = null,

    var accelerometer : AxisSpecificGauge? = null,
    var magneticFieldMicroTesla : AxisSpecificGauge? = null,
    var gyroscopeRadiansPerSecond: AxisSpecificGauge? = null,

    var ambientLightLux : Double? = null,
    var pressureHectoPascal : Double? = null,
    var proximityCentimeters : Double? = null,

    var gravityAcceleration : AxisSpecificGauge? = null,
    var linearAcceleration : AxisSpecificGauge? = null,
    var rotationVectorValues : AxisSpecificGauge? = null,
    var rotationVectorCosinusThetaHalf : Double? = null,
    var rotationVectorAccuracyRadians : Double? = null,
);

//TODO for cpu, use HardwarePropertiesManager

private val supportedSensors : List<Int> = listOf(
    Sensor.TYPE_HEADING,
    Sensor.TYPE_HINGE_ANGLE,
    Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT,
    Sensor.TYPE_AMBIENT_TEMPERATURE,
    Sensor.TYPE_RELATIVE_HUMIDITY,
    Sensor.TYPE_ACCELEROMETER,
    Sensor.TYPE_MAGNETIC_FIELD,
    Sensor.TYPE_GYROSCOPE,
    Sensor.TYPE_LIGHT,
    Sensor.TYPE_PRESSURE,
    Sensor.TYPE_PROXIMITY,
    Sensor.TYPE_GRAVITY,
    Sensor.TYPE_LINEAR_ACCELERATION,
    Sensor.TYPE_ROTATION_VECTOR,
)

val temperatureTypes : Map<Int, String> = mapOf(
    HardwarePropertiesManager.DEVICE_TEMPERATURE_BATTERY to "battery",
    HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU to "cpu",
    HardwarePropertiesManager.DEVICE_TEMPERATURE_GPU to "gpu",
    HardwarePropertiesManager.DEVICE_TEMPERATURE_SKIN to "skin",

)

class MetricsEngine(private val context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val hwSensorsCache = HwSensorsCache()
    val hwPropertiesManager = context.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE) as HardwarePropertiesManager

    init {
        registerAllHwEventHandlers()
    }

    fun hwSensorsValues(): HwSensorsCache {
        return hwSensorsCache
    }

    private fun registerAllHwEventHandlers() {
        Log.d(TAG, "Registering all hardware sensors")

        //TODO test registering and unregistering as optimization

        supportedSensors.forEach { supportedSensor ->
            val sensor: Sensor? = sensorManager.getDefaultSensor(supportedSensor)
            sensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    fun dispose() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) {
            return
        }
        if (event.values == null) {
            return
        }

        when (event.sensor.type) {
            Sensor.TYPE_HEADING -> {
                hwSensorsCache.headingDegrees = event.values[0].toDouble()
                hwSensorsCache.headingAccuracyDegrees = event.values[1].toDouble()
            }

            Sensor.TYPE_HINGE_ANGLE -> {
                hwSensorsCache.hingeAngleDegrees = event.values[0].toDouble()
            }

            Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT -> {
                hwSensorsCache.offbodyDetect = event.values[0].toDouble()
            }

            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                hwSensorsCache.ambientTemperatureCelsius = event.values[0].toDouble()
            }

            Sensor.TYPE_RELATIVE_HUMIDITY -> {
                hwSensorsCache.relativeHumidityPercent = event.values[0].toDouble()
            }

            Sensor.TYPE_ACCELEROMETER -> {
                hwSensorsCache.accelerometer = valuesToAxisSpecificGauge(event.values)
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                hwSensorsCache.magneticFieldMicroTesla = valuesToAxisSpecificGauge(event.values)
            }

            Sensor.TYPE_GYROSCOPE -> {
                hwSensorsCache.gyroscopeRadiansPerSecond = valuesToAxisSpecificGauge(event.values)
            }

            Sensor.TYPE_LIGHT -> {
                hwSensorsCache.ambientLightLux = event.values[0].toDouble()
            }

            Sensor.TYPE_PRESSURE -> {
                hwSensorsCache.pressureHectoPascal = event.values[0].toDouble()
            }

            Sensor.TYPE_PROXIMITY -> {
                hwSensorsCache.proximityCentimeters = event.values[0].toDouble()
            }

            Sensor.TYPE_GRAVITY -> {
                hwSensorsCache.gravityAcceleration = valuesToAxisSpecificGauge(event.values)
            }

            Sensor.TYPE_LINEAR_ACCELERATION -> {
                hwSensorsCache.linearAcceleration = valuesToAxisSpecificGauge(event.values)
            }

            Sensor.TYPE_ROTATION_VECTOR -> {
                hwSensorsCache.rotationVectorValues = valuesToAxisSpecificGauge(event.values)
                hwSensorsCache.rotationVectorCosinusThetaHalf = event.values[3].toDouble()
                hwSensorsCache.rotationVectorAccuracyRadians = event.values[4].toDouble()
            }
        }
    }

    private fun valuesToAxisSpecificGauge(values: FloatArray): AxisSpecificGauge {
        return AxisSpecificGauge(
            x = values[0].toDouble(),
            y = values[1].toDouble(),
            z = values[2].toDouble(),
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing
    }


//    - network availability
//    - 4G, 5G
//    - ram
//    - scrape duration
//    - bluetooth - mac bluetooth
//    - storage information

    fun batteryChargeRatio(): Float {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { intFilter ->
            context.registerReceiver(null, intFilter)
        }
        //val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val batteryRatio: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level / scale.toFloat()
        }

        batteryRatio ?: return -1.0f
        return batteryRatio
    }

    fun getNumberOfCpuCores(): Int {
        return Os.sysconf(OsConstants._SC_NPROCESSORS_CONF).toInt()
    }

    fun getUptimeInSeconds(): Double {
        return SystemClock.elapsedRealtime() / 1000.0
    }

    fun cpuUsage() : Array<CpuUsageInfo> {
        return hwPropertiesManager.cpuUsages
    }

    fun getDeviceTemperatures() : Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        temperatureTypes.keys.forEach { type ->
            val array = hwPropertiesManager.getDeviceTemperatures(
                type,
                HardwarePropertiesManager.TEMPERATURE_CURRENT
            )
            if(array.isNotEmpty()){
                result[temperatureTypes[type]!!] = array[0].toDouble()
            }
        }
        return result
    }

    fun getAndroidOsVersion(): String{
        return Build.VERSION.RELEASE
    }

    fun getAndroidModel(): String {
        return Build.MODEL
    }

    fun getAndroidManufacturer() : String {
        return Build.MANUFACTURER
    }

    //TODO has_celular
    //TODO has_wifi
}
