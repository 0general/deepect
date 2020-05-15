package kr.ac.inu.deepect.arnavigation.navigation

import android.util.Log

class LogManager {
    companion object {
        val _DEBUG : Boolean = true
        private val _TAG : String = "TMap"

        fun printLog(text : String) {
            if(_DEBUG) {
                Log.d(_TAG, text)
            }
        }

        fun printError(text : String) {
            if(_DEBUG){
                Log.e(_TAG,"ERROR :: $text" )
            }
        }
    }
}