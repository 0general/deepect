package kr.ac.inu.deepect.arnavigation.navigation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kr.ac.inu.deepect.arnavigation.navigation.MainActivity
import kr.ac.inu.deepect.R

class SplashScreen : Activity() {

    private final val REQUEST_USED_PERMISSION = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_layout)

        requestPermissions()

    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(
                    this, arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ), REQUEST_USED_PERMISSION
            )
        } else {
            skipSplashScreen()
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        for (res in grantResults) {
            if (res != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "권한을 허용해야 내비게이션 서비스를 이용할 수 있습니다.", Toast.LENGTH_SHORT).show()
                requestPermissions()
                return
            }
        }

        skipSplashScreen()
    }


    private fun skipSplashScreen() {
        val handler = Handler()
        handler.postDelayed(object : Runnable{
            override fun run() {
                val intent = Intent(this@SplashScreen, MainActivity::class.java)

                startActivity(intent)
                finish()
            }
        }, 2000)

    }
}