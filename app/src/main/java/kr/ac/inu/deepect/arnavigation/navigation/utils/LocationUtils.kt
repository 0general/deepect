package kr.ac.inu.deepect.arnavigation.navigation.utils

import android.location.Location
import kr.ac.inu.deepect.arnavigation.navigation.DirectionManager

class LocationUtils{
    fun directionBetween(src : Location , dest : Location) : Float {
        val directionManager = DirectionManager
        return directionManager.instance!!.getDirectionBetween(src, dest)
    }
    fun distanceBetween(src : Location , dest : Location) : Float {
        return src.distanceTo(dest)
    }
}