package kr.ac.inu.deepect.arnavigation.navigation

import android.location.Location
import android.util.Log

class Vector(start: Location, end: Location) {
    var direction = 0.0
    var distance = 0.0

    init {
        try {
            val results = FloatArray(3)
            Location.distanceBetween(
                    start.latitude, start.longitude,
                    end.latitude, end.longitude, results
            )
            distance = results[0].toDouble()
            //direction = DirectionManager.getInstance().getDirectionBetween(start, end)
            Log.d("== Vector Result ==", "")
            Log.d("results(distance)::", distance.toString())
            //Log.d("results(direction)::", direction.toString())
            Log.d("===================", "")
        } catch (ex: Exception) {
            Log.d("Exception: ", "can't calculate vector.")
        }
    }
}