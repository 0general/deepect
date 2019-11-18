package com.example.navigation

import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import android.os.Bundle
import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast


import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import java.lang.reflect.Array
import java.util.*
import java.io.IOException
import java.lang.IllegalArgumentException
import java.lang.*


class MainActivity : AppCompatActivity(),
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private var mMap: GoogleMap? = null
    private var currentMarker : Marker? = null

    private val TAG: String = "googlemap_example"
    private val GPS_ENABLE_REQUEST_CODE = 2001
    private val UPDATE_INTERVAL_MS = 1000L
    private val FASTEST_UPDATE_INTERVAL_MS = 500L

    // onRequestPermissionsResult에서 수신된 결과에서 ActivityCompat.requestPermissions 를 사용한 퍼미션 요청을 구별하기 위해 사용됩니다.
    private val PERMISSIONS_REQUUEST_CODE = 100
    var needRequest = false

    //앱을 실행하기 위해 필요한 퍼미션을 정의
    var REQUESTD_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    lateinit var mCurrentLocation: Location
    lateinit var currentPosition: LatLng

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var location: Location

    private lateinit var mLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(R.layout.activity_main)

        mLayout = findViewById(R.id.layout_main)

        locationRequest = LocationRequest()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(UPDATE_INTERVAL_MS)
            .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS)

        val builder: LocationSettingsRequest.Builder =
            LocationSettingsRequest.Builder()

        builder.addLocationRequest(locationRequest)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(TAG, "onMapReady : ")

        mMap = googleMap

        //런타임 퍼미션 요청 대화상자나 GPS 활성 요청 대화상자 보이기전에
        //지도의 초기위치를 서울로 이동
        setDefaultLocation()

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        val hasFineLocationPermission: Int =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarseLocationPermission: Int =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
            hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)

            startLocationUpdates()

        } else { //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    REQUESTD_PERMISSIONS[0])) {

                // 3-2. 요청을 진행하기 전에 사용자에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Snackbar.make(
                    mLayout, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("확인",object : View.OnClickListener{
                    override fun onClick(v: View?) {
                        ActivityCompat.requestPermissions(this@MainActivity, REQUESTD_PERMISSIONS, PERMISSIONS_REQUUEST_CODE)
                    }
                }).show()
            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult 에서 수신됩니다.
                ActivityCompat.requestPermissions(
                    this,
                    REQUESTD_PERMISSIONS,
                    PERMISSIONS_REQUUEST_CODE
                )
            }
        }

        mMap?.uiSettings?.isMyLocationButtonEnabled
        mMap?.animateCamera(CameraUpdateFactory.zoomTo(15F))
        mMap?.setOnMapClickListener(object : GoogleMap.OnMapClickListener{
            override fun onMapClick(p0: LatLng?) {
                Log.d(TAG, "onMapClick : ")
            }
        })
    }

    val locationCallback : LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val locationList: List<Location> = locationResult.locations

            if (locationList.size > 0) {
                location = locationList.get(locationList.size - 1)
                //location = locationList.get(0)

                currentPosition = LatLng(location.latitude, location.longitude)

                val markerTitle = getCurrentAddress(currentPosition)
                val markerSnippets = "위도 :" + location.latitude.toString() +
                        "경도:" + location.longitude.toString()

                Log.d(TAG, "onLocationResult : " + markerSnippets)

                setCurrentLocation(location, markerTitle, markerSnippets)
                mCurrentLocation = location
            }
        }

    }

    fun startLocationUpdates() : Unit {
        if (!checkLocationServicesStatus()) {
            Log.d(TAG, "startLocationUpdates : call showDialogForLocationServiceSetting")
            showDialogForLocationServiceSetting()
        } else {
            val hasFineLocationPermission : Int =  ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
            val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)

            if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED ||
                    hasCoarseLocationPermission != PackageManager.PERMISSION_DENIED){
                Log.d(TAG, "startLocationUpdates : 퍼미션 안가지고 있음")
                return
            }

            Log.d(TAG, "startLocationUpdates : call" +
                    "mFusedLocationClient.requestLocationUpdates")
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
            if (checkPermission())
                mMap?.isMyLocationEnabled
        }
    }

    override fun onStart() {
        super.onStart()

        Log.d(TAG, "onStart")
        if (checkPermission()) {
            Log.d(TAG, "onStart : call mFusedLocationClient.requestLocationUpdates")
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

            if (mMap!=null)
                mMap?.setMyLocationEnabled(true);
        }
    }

    override fun onStop() {
        super.onStop()

        if (mFusedLocationClient != null) {
            Log.d(TAG, "onStop : call stopLocationUpdates")
            mFusedLocationClient.removeLocationUpdates(locationCallback)

        }
    }

    fun getCurrentAddress(latlng: LatLng) : String {

        val geocoder = Geocoder(this, Locale.getDefault())

        val addresses : List<Address>

        try {
            addresses = geocoder.getFromLocation(
                latlng.latitude,
                latlng.longitude,
                1
            )
        } catch (ioException : IOException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show()
            return "지오코더 서비스 사용불가"
        } catch(illegalArgumentException : IllegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show()
            return "잘못된 GPS 좌표"
        }

        if (addresses == null || addresses.size == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show()
            return "주소 미발견"
        } else {
            val address : Address = addresses.get(0)
            return address.getAddressLine(0).toString()
        }
    }

    public fun checkLocationServicesStatus() : Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    public fun setCurrentLocation(location : Location , markerTitle : String, markerSnippet : String) {
        if (currentMarker != null) currentMarker?.remove()

        val currentLatLng = LatLng(location.latitude, location.longitude)

        val markerOptions = MarkerOptions()
        markerOptions.position(currentLatLng)
        markerOptions.title(markerTitle)
        markerOptions.snippet(markerSnippet)
        markerOptions.draggable(true)

        currentMarker = mMap?.addMarker(markerOptions)

        val cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng)
        mMap?.moveCamera(cameraUpdate)


    }

    public fun setDefaultLocation() {

        //디폴트 위치, Seoul
        var DEFAULT_LOCATION : LatLng = LatLng(37.56, 126.97)
        val markerTitle : String = "위치정보 가져올 수 없음"
        val markerSnippet : String = "위치 퍼미션과 GPS 활성 요부 확인하세요"

        if (currentMarker != null) currentMarker?.remove()

        val markerOptions = MarkerOptions()
        markerOptions.position(DEFAULT_LOCATION)
        markerOptions.title(markerTitle)
        markerOptions.snippet(markerSnippet)
        markerOptions.draggable(true)

        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        currentMarker = mMap?.addMarker(markerOptions)

        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 15F)
            mMap?.moveCamera(cameraUpdate)
    }

    //여기서부터는 런타임 퍼미션 처리를 위한 메소드들
    private fun checkPermission() : Boolean {
        val hasFineLocationPermission : Int = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarseLocationPermission : Int = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION)

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        return false
    }

    override fun onRequestPermissionsResult(
        permsRequestCode : Int,
        permissions: kotlin.Array<out String>,
        grandResults: IntArray
    ) {
        super.onRequestPermissionsResult(permsRequestCode, permissions, grandResults)
        if(permsRequestCode == PERMISSIONS_REQUUEST_CODE && grandResults.size ==
            REQUESTD_PERMISSIONS.size){

            //요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면

            var check_result : Boolean = true

            // 모든 퍼미션을 허용했는지 체크합니다.

            for (result in grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false
                    break
                }
            }

            if (check_result) {

                //퍼미션을 허용했다면 위치 업데이트를 시작합니다.
                startLocationUpdates()
            } else {
                //거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명하고 앱을 종료. 2가지 경우가있음
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, REQUESTD_PERMISSIONS[0]) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(this, REQUESTD_PERMISSIONS[1])) {

                    //사용자가 거부만 선택한 경우에는 앱을 다시 실행하여 허용을 선택하면 앱을 사용할 수 있습니다.

                    Snackbar.make(mLayout, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요",
                        Snackbar.LENGTH_INDEFINITE).setAction("확인", object : View.OnClickListener {
                        override fun onClick(v: View?) {
                            finish()
                        }
                    }).show()
                } else {

                    //"다시 묻지 않음"을 사용자가 체크하고 거부를 선택한 경우에는 설정(앱 정보) 에서 퍼미션을 허용해야 앱을 사용할 수 있습니다.
                    Snackbar.make(mLayout, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다.",
                        Snackbar.LENGTH_INDEFINITE).setAction("확인", object : View.OnClickListener{
                        override fun onClick(v: View?) {
                            finish()
                        }
                    }).show()
                }
            }
        }

    }

    private  fun showDialogForLocationServiceSetting() {

        val builder : AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("위치 서비스 비활성화")
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n" + "위치 수정하실?")
        builder.setCancelable(true)
        builder.setPositiveButton("설정", object : DialogInterface.OnClickListener{
            override fun onClick(dialog: DialogInterface?, id: Int) {
                val callGPSSettingIntent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE)
            }
        })
        builder.setNegativeButton("취소", object : DialogInterface.OnClickListener{
            override fun onClick(dialog: DialogInterface, id: Int) {
                dialog.cancel()
            }
        })
        builder.create().show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode){
            GPS_ENABLE_REQUEST_CODE ->
                if (checkLocationServicesStatus()) {
                    if(checkLocationServicesStatus()){
                        Log.d(TAG, "onActivityresult : GPS 활성화 되었음")

                        needRequest = true

                        return
                    }
                }
        }

    }
}



