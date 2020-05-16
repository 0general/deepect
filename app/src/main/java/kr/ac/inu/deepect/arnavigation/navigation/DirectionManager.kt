package kr.ac.inu.deepect.arnavigation.navigation

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.util.Log

class DirectionManager constructor() {
    companion object {
        var instance: DirectionManager

        init {
            instance = DirectionManager()
        }
    }

    private var accData: FloatArray? = null
    private var magData: FloatArray? = null
    var pitch = 0.0f
        private set
    var roll = 0.0f
        private set
    var rotation = FloatArray(9)
    var resultData = FloatArray(3)

    var sensorEventListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val values = event.values.clone()
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> instance!!.accData =
                        values
                Sensor.TYPE_MAGNETIC_FIELD -> instance!!.magData =
                        values
                Sensor.TYPE_ORIENTATION -> {
                    instance!!.pitch = values[1]
                    instance!!.roll = values[2]
                }
            }
        }

        override fun onAccuracyChanged(
                sensor: Sensor,
                i: Int
        ) {
        }
    }


    fun getAzinuth() : Float {
        if (accData != null && magData != null) {
            SensorManager.getRotationMatrix(rotation, null, accData, magData)
            SensorManager.getOrientation(rotation, resultData)
            resultData[0] = Math.toDegrees(resultData[0].toDouble()).toFloat()
            if (resultData[0] < 0) {
                resultData[0] += 360F
            }
            return resultData[0]
        } else {
            Log.d("Info::", "accData and magData must not be null!")
        }
        return 0F
    }

    fun getDirectionBetween(
            src: Location,
            dest: Location?
    ): Float {
        val azimuth: Float
        val bearing: Float
        var relative = 0.0f
        try {
            azimuth = getAzinuth()
            bearing = src.bearingTo(dest)
            relative = bearing - azimuth
        } catch (ex: Exception) {
            Log.d("Exception::", "can not read GpsManager instance")
        }
        return relative
    }

    fun getAccData(): FloatArray? {
        return accData
    }

    fun getMagData(): FloatArray? {
        return magData
    }

    fun setAccData(accData: FloatArray?) {
        this.accData = accData
    }

    fun setMagData(magData: FloatArray?) {
        this.magData = magData
    }

    fun getInstance() : DirectionManager {
        return instance
    }


}