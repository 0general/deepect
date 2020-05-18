package kr.ac.inu.deepect.arnavigation.navigation

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.skt.Tmap.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kr.ac.inu.deepect.R
import kr.ac.inu.deepect.arnavigation.ARActivity
import kr.ac.inu.deepect.arnavigation.navigation.*
import kr.ac.inu.deepect.arnavigation.navigation.ParseJson.Companion.parseJSON
import kr.ac.inu.deepect.arnavigation.navigation.utils.LocationUtils
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(),  NavigationView.OnNavigationItemSelectedListener {

    private var selectionMode = false
    private var navigationMode = false

    private var oldMarker: TMapMarkerItem? = null
    private var destination : TMapPoint? = null

    private lateinit var mapView : TMapView

    private lateinit var gpsManager: GpsManager
    private lateinit var pathManager: PathManager
    private lateinit var directionManager: DirectionManager

    private lateinit var timer : Timer
    private lateinit var timerTask: TimerTask

    private var backPressTime : Long = 0

    var Start_Point : TMapPoint? = null
    var Destination_Point : TMapPoint? = null


    var start  = false

    var controller : Int = 0


    private lateinit var Address : String
    private var m_Latitude = 0.0
    private var m_Longitude = 0.0

    private val REQUEST_SEARCH = 0x0001
    private val REQUEST_HISTORY = 0x0002




    //Marker
    private var markerIdList = ArrayList<String>()

    //PopupList Item List
    //private var popupListItems = ArrayList<PopupListItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        mapView = TMapView(this)

        try {
            //gps = TMapGpsManager(this)
            initView()

            GpsManager.init(this)
            gpsManager = GpsManager.getInstance()
            gpsManager.setOnLocationListener(locationListener)


            pathManager = PathManager.getInstance()

            directionManager = DirectionManager().getInstance()


            checkPermission()


            val button = findViewById<Button>(R.id.button)
            button.setOnClickListener{
                val intent = Intent(this, ARActivity::class.java)
                startActivity(intent)
            }


            /*btnPath.setOnClickListener {
                setNavigationMode(true)
            }
*/
            moveToCurrentLocation()
        } catch (e: Exception){
            Log.d("Exception : ", "cant initialize ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            gpsManager.start()
        } catch (e: Exception) {

        }

    }

    override fun onPause() {
        super.onPause()
        try{
            gpsManager.stop()
        } catch (e: Exception){

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try{
            timer.cancel()
            gpsManager.stop()
        } catch(e : Exception){

        }

    }



    private fun checkPermission() {
        if(ActivityCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            return
        }
    }

    private fun initView() {
        setSupportActionBar(toolbar)

        //setMapIcon()
        mapView.setSKTMapApiKey(getString(R.string.tmap_api_key))
        //mapView.setLocationPoint(gps.location.longitude, gps.location.latitude)
        setSelectionMode(!selectionMode)
        mapView.setIconVisibility(true)
        mapView.setCompassMode(false)

        frameMap.addView(mapView)

        mapView.setOnApiKeyListener(object : TMapView.OnApiKeyListenerCallback{
            override fun SKTMapApikeySucceed() {
                Log.d("SKTMapApikeySucceed", "ApiSucceed")
            }

            override fun SKTMapApikeyFailed(errorMsg: String?) {
                Log.d("SKTMapApikeyFailed " ,errorMsg)
            }
        })

        btnPath.setOnClickListener(btnPathClicked)
        btnNow.setOnClickListener(btnNowClicked)
        switch1.setOnCheckedChangeListener(btnSwitched)
        switch1.visibility = View.GONE
        timelayout.visibility = View.GONE

        val toggle = ActionBarDrawerToggle(
                this,
                drawer_layout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        )
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)


    }

    fun setMapIcon() {
        val currentMarker = TMapMarkerItem()
        //val bitmap = BitmapFactory.decodeResource(this.resources, R.drawable.poi_here)
        //mapView.setIcon(bitmap)
        mapView.setZoomLevel(16)
        mapView.addMarkerItem("CurrentMarker", currentMarker)

    }


    private fun moveToCurrentLocation() {
        try{
            val currentLocation = gpsManager.getCurrentLocation()
            if (currentLocation != null) {
                mapView.setLocationPoint(currentLocation.longitude, currentLocation.latitude)
                mapView.setCenterPoint(currentLocation.longitude, currentLocation.latitude)
            }
        } catch (ex : Exception) {
            Toast.makeText(this, "현재 위치를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateDirection() {

        if(controller == 1){
            val distThreshold = 20.0f // 20 meter

            var nearestPoint: TMapPoint? = null
            try {

                nearestPoint = pathManager.nearestPoint
                Log.d("nearest:", "" + pathManager.getNearestIndex() + pathManager.nearestPoint);
                //double distance = nearestVector.getDistance();
                val currentLocation = gpsManager.getCurrentLocation()
                val nearestLocation = Location(LocationManager.GPS_PROVIDER)
                nearestLocation.longitude = nearestPoint!!.longitude
                nearestLocation.latitude = nearestPoint.latitude
                val distance: Float =
                        LocationUtils().distanceBetween(currentLocation, nearestLocation)

                //Log.d("distance:", "" + distance);

                if (distance < distThreshold) {
                    val nearestIndex : Int = pathManager.nearestIndex

                    // remove nearest marker and marker id
                    val targetMarkerId = markerIdList[nearestIndex]
                    mapView.removeMarkerItem(targetMarkerId)
                    markerIdList.removeAt(nearestIndex)

                    if (pathManager.hasNext()) { // Path has next point
                        nearestPoint = pathManager.nearestPoint
                        Log.d("nearestPoint", "${nearestPoint}" )

                    } else { // 여기서부터 조져야돼
                        Log.d("여기가 오냐","작동되긴해?")
                        timer.cancel()
                        val builder = AlertDialog.Builder(this)
                                .setTitle("안내")
                                .setMessage("목적지에 도착했습니다")
                                .setPositiveButton("확인", object : DialogInterface.OnClickListener{
                                    override fun onClick(dialog: DialogInterface?, which: Int) {
                                        nearestPoint = null
                                        mapView.removeMarkerItem("도착지")
                                        destination = null
                                        oldMarker = null
                                        setNavigationMode(!navigationMode)
                                        setSelectionMode(true)
                                        return
                                    }
                                }).show()

                    }
                } else { // out of 20.0 meters

                }

            } catch (ex: java.lang.Exception) {
                Log.d("Exception:", ex.message)
            }
        } else {
            Log.d("controller", "${controller}")
        }

    }



    private fun setSelectionMode(isSelectionMode : Boolean) {
        this.selectionMode = isSelectionMode

        if (isSelectionMode) {
            toolbar.setTitle("도착지를 설정하세요")
            switch1.visibility = View.GONE
            timelayout.visibility = View.GONE

            mapView.setOnLongClickListenerCallback(object : TMapView.OnLongClickListenerCallback {
                override fun onLongPressEvent(
                        p0: ArrayList<TMapMarkerItem>?,
                        p1: ArrayList<TMapPOIItem>?,
                        tMapPoint: TMapPoint
                ) {
                    val builder = AlertDialog.Builder(this@MainActivity)
                            .setTitle("안내")
                            .setMessage("도착지로 설정하시겠습니까?")
                            .setPositiveButton("예", object : DialogInterface.OnClickListener {
                                override fun onClick(dialog: DialogInterface?, which: Int) {
                                    setDestination(tMapPoint)

                                    // turn off selection mode
                                    setSelectionMode(false);

                                    //getRoute(tMapPoint)

                                }
                            })
                            .setNegativeButton("아니오", null).show()
                }
            })


        } else {
            mapView.setOnLongClickListenerCallback(null)
        }
    }


    private fun setNavigationMode(isNavigationMode: Boolean) {
        this.navigationMode = isNavigationMode

        if (isNavigationMode) {
            try {
                if (destination == null) {
                    Toast.makeText(this, "먼저 도착지를 선택하세요.", Toast.LENGTH_SHORT).show()
                    setNavigationMode(false)
                    setSelectionMode(true)
                    return
                }
                val mapData = TMapData()

                switch1.visibility = View.VISIBLE
                timelayout.visibility = View.VISIBLE


                val currentLocation = gpsManager.currentLocation
                if (currentLocation != null) {
                    mapView.setLocationPoint(currentLocation.longitude, currentLocation.latitude)
                    mapView.setCenterPoint(currentLocation.longitude, currentLocation.latitude)
                }

                val startPoint = TMapPoint(currentLocation!!.latitude, currentLocation.longitude)

                val endpoint = destination
                //getRoute(endpoint!!)

                val routeApi =
                        RouteApi(this, startPoint, endpoint!!, object : RouteApi.EventListener {
                            override fun onApiResult(jsonString: String) {
                                Toast.makeText(this@MainActivity, "요청성공", Toast.LENGTH_SHORT).show()
                                try {
                                    val objects: JSONObject = JSONObject(jsonString)
                                    Log.d("result:", objects.toString())

                                    val polyLine = parseJSON(objects)
                                    Log.d("totaltime","${ParseJson.totalTime}")

                                    val now = System.currentTimeMillis()
                                    Log.d("now", "${now}")
                                    val expectedtime : Long = now + (ParseJson.totalTime!!.toLong()*1000L)
                                    Log.d("et", "${expectedtime}")
                                    val date1 =Date(expectedtime)
                                    val sdfNow = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                                    val formatDate = sdfNow.format(date1)
                                    totaltime.text = formatDate


                                    runOnUiThread(object : Runnable {
                                        override fun run() {
                                            val linePoints: ArrayList<TMapPoint> =
                                                    polyLine.getLinePoint()
                                            markerIdList.clear()

                                            var i = 0
                                            mapView.removeAllMarkerItem()

                                            for (p: TMapPoint in linePoints) {
                                                val markerItem = TMapMarkerItem()
                                                markerItem.tMapPoint = p

                                                val id = "i" + i++
                                                Log.d("id", id)
                                                markerItem.id = id

                                                //mapView.addMarkerItem(id, markerItem)

                                                markerIdList.add(id)
                                            }

                                            mapView.addTMapPath(polyLine)


                                            pathManager.setPolyLine(polyLine)

                                            controller = 1

                                        }
                                    })
                                } catch (ex: Exception) {
                                    Log.d("EXC", ex.message)
                                }

                            }

                            override fun onApiFailed() {
                                Toast.makeText(this@MainActivity, "요청실패", Toast.LENGTH_SHORT).show()
                            }
                        })
                routeApi.start()

                setTrackingMode()
                setSelectionMode(false)
                //mapView.setUserScrollZoomEnable(true);
                //mapView.setCompassMode(true)




                Toast.makeText(this, "길 안내를 시작합니다.", Toast.LENGTH_SHORT).show()
                // always compass mode
                /*mapView.setOnTouchListener { v, event ->
                    mapView.setCompassMode(true)
                    true
                }*/

                moveToCurrentLocation()


                timer()

                Log.d("Start", "$start")

            } catch (ex: Exception) {
                Log.d("EXC", ex.message)
                setNavigationMode(false)
            } finally {
                //btnPath.visibility = View.VISIBLE
            }

        } else {

            controller = 0
            mapView.setUserScrollZoomEnable(false);
            mapView.setCompassMode(false);
            mapView.setOnTouchListener(null)
            setSelectionMode(true)
            mapView.removeTMapPath();
            mapView.removeAllMarkerItem();

            if (oldMarker != null) {
                mapView.addMarkerItem("도착지", oldMarker);
            }

            try {
                timer.cancel()
            } catch (e: Exception) {
                Log.d("error", "${e.message}")
            }
        }
    }

    private fun timer() {
        timer = Timer(true)
        timerTask = object : TimerTask() {
            override fun run() {
                val mHandler = Handler(Looper.getMainLooper())
                mHandler.postDelayed(object : Runnable{
                    override fun run() {
                        updateDirection()
                    }
                }, 0)
            }
        }
        timer.schedule(timerTask, 300, 500)

    }

    private fun getRoute(endPoint: TMapPoint){

        val currentLocation : Location? = gpsManager.getCurrentLocation()
        val startPoint = TMapPoint(currentLocation!!.latitude , currentLocation!!.longitude)

        if(startPoint != null) {
            val routeApi = RouteApi(this, startPoint, endPoint, object : RouteApi.EventListener {
                override fun onApiResult(jsonString : String) {
                    Toast.makeText(this@MainActivity, "요청성공", Toast.LENGTH_SHORT).show()
                    try {
                        val objects : JSONObject = JSONObject(jsonString)
                        Log.d("result:", objects.toString())

                        val polyLine = parseJSON(objects)

                        runOnUiThread(object : Runnable {
                            override fun run() {
                                val linePoints : ArrayList<TMapPoint> = polyLine.getLinePoint()
                                markerIdList.clear()

                                var i = 0
                                mapView.removeAllMarkerItem()

                                for (p : TMapPoint in linePoints) {
                                    val markerItem = TMapMarkerItem()
                                    markerItem.tMapPoint = p

                                    val id = "i" + i++
                                    Log.d("id", id)
                                    markerItem.id = id

                                    mapView.addMarkerItem(id, markerItem)

                                    markerIdList.add(id)
                                }

                                mapView.addTMapPath(polyLine)

                                pathManager.setPolyLine(polyLine)

                            }
                        })
                    } catch (ex : Exception) {
                        Log.d("EXC", ex.message)
                    }

                }

                override fun onApiFailed() {
                    Toast.makeText(this@MainActivity, "요청실패", Toast.LENGTH_SHORT).show()
                }
            })
            routeApi.start()
        }
    }

    private fun setDestination(destination : TMapPoint) {
        //clear previous destination
        try{
            mapView.removeMarkerItem("도착지")
        } catch (ex : Exception){

        }

        this.destination = destination
        Destination_Point = destination
        ARActivity.setDestination(destination)


        //add destination marker on TMap View
        val marker = TMapMarkerItem()
        marker.id = "도착지"
        marker.tMapPoint = destination
        oldMarker = marker;

        mapView.addMarkerItem("도착지", marker)
        Log.d("Info::", "도착지 설정 완료")
    }

    fun setTrackingMode() {mapView.setTrackingMode(mapView.getIsTracking())}


    override fun onBackPressed() {

        if(drawer_layout.isDrawerOpen(GravityCompat.START)){
            drawer_layout.closeDrawer(GravityCompat.START)
        } else if(navigationMode){
            timer.cancel()
            setNavigationMode(false)
            mapView.removeMarkerItem("도착지")
            destination=null
            setSelectionMode(true)
        } else if (!navigationMode && oldMarker != null){
            mapView.removeMarkerItem("도착지")
            destination = null
            oldMarker = null
            setSelectionMode(true)
        }
        else if (selectionMode) {

            val currentTime = System.currentTimeMillis()

            if(currentTime - backPressTime < 2000) {
                finish()
            }else {
                backPressTime = currentTime
                Toast.makeText(this, "종료하려면 뒤로가기 버튼을 누르세요.", Toast.LENGTH_SHORT).show();
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == Activity.RESULT_OK){
            when(requestCode){
                REQUEST_SEARCH -> {
                    setNavigationMode(false)

                    val name = data?.getStringExtra("POI")
                    val longitude = data?.getDoubleExtra("LON", 0.0)
                    val latitude = data?.getDoubleExtra("LAT", 0.0)

                    if(latitude != null && longitude != null) {
                        val mapPoint = TMapPoint(latitude, longitude)
                        setDestination(mapPoint)
                        mapView.setCenterPoint(longitude, latitude)
                        appendToHistoryFile(name!!, latitude, longitude)
                    }


                }
                REQUEST_HISTORY ->{
                    setNavigationMode(false)

                    val name = data?.getStringExtra("POI")
                    val longitude = data?.getDoubleExtra("LON", 0.0)
                    val latitude = data?.getDoubleExtra("LAT", 0.0)

                    if(latitude != null && longitude != null) {
                        val mapPoint = TMapPoint(latitude, longitude)
                        setDestination(mapPoint)
                        mapView.setCenterPoint(longitude, latitude)
                    }

                }
                else -> {
                    super.onActivityResult(requestCode, resultCode, data)
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        try {
            when(id) {
                R.id.nav_search -> {
                    val intent = Intent(this , SearchActivity::class.java)
                    startActivityForResult(intent, REQUEST_SEARCH)
                }
                R.id.nav_history -> {
                    val intent = Intent(this, HistoryActivity::class.java)
                    startActivityForResult(intent, REQUEST_HISTORY)
                }
            }
        } catch( e : Exception){
            Log.d("Exception:", e.message)
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun appendToHistoryFile(name : String, latitude : Double , longitude : Double) {
        try {
            val bw = BufferedWriter(FileWriter(File(filesDir, "history.txt"), true))
            bw.append(String.format("%s %f %f", name, longitude, latitude))
            bw.newLine()
            bw.close()
        } catch (e : Exception){
            Log.d("FileWirteException", e.message)
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    val locationListener = object : LocationListener{
        override fun onLocationChanged(location: Location?) {
            try {
                if(location != null) {
                    val distanceFromPrev: Float = location.distanceTo(gpsManager.getLastLocation())

                    if ((distanceFromPrev < 20.0f) || (distanceFromPrev > 100.0f)) {
                        gpsManager.setLastLocation(location)

                        mapView.setLocationPoint(location.longitude , location.latitude)
                        if(navigationMode){
                            moveToCurrentLocation()
                            //updateDirection()
                        }
                    }
                }
            } catch (e : Exception){ }
        }

        override fun onProviderDisabled(provider: String?) {
            // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            Log.d("Exception1 : ", provider)
        }

        override fun onProviderEnabled(provider: String?) {
            // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            Log.d("Exception2 : ", provider)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            Log.d("Exception3 : ", provider)
        }
    }

    private val btnPathClicked = object : View.OnClickListener {
        override fun onClick(v: View?) {
            try{
                setNavigationMode(!navigationMode)

            } catch(e: Exception){
                Log.d("Exception : ", e.message)
            }
        }
    }

    private val btnNowClicked = object : View.OnClickListener {
        override fun onClick(v: View?) {
            moveToCurrentLocation()
        }
    }

    private val btnSwitched = object : CompoundButton.OnCheckedChangeListener{
        override fun onCheckedChanged(p0: CompoundButton?, isChecked: Boolean) {
            if(isChecked){
                mapView.setCompassMode(true)
            }else {
                mapView.setCompassMode(false)
            }
        }
    }
}