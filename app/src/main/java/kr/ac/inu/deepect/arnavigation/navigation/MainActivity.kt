package kr.ac.inu.deepect.arnavigation.navigation

import android.app.Activity
import android.app.Dialog
import android.content.Context
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
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.skt.Tmap.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kr.ac.inu.deepect.R
import kr.ac.inu.deepect.arnavigation.ARActivity
import kr.ac.inu.deepect.arnavigation.navigation.*
import kr.ac.inu.deepect.arnavigation.navigation.ParseJson.Companion.parseJSON
import kr.ac.inu.deepect.arnavigation.navigation.utils.LocationUtils
import kr.ac.inu.deepect.arnavigation.navigation.utils.StringUtils
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

    var Now_Point : TMapPoint? = null
    var Restart_Point : TMapPoint? = null



    var start  = false

    var controller : Int = 0
    var aroundcontroller : Int = 0




    private lateinit var Address : String
    private var m_Latitude = 0.0
    private var m_Longitude = 0.0

    private val REQUEST_SEARCH = 0x0001
    private val REQUEST_HISTORY = 0x0002
    private val REQUEST_AROUND = 0x0003
    private val REQUEST_AR = 0x0004


    lateinit var adapter: SearchListAdapter
    lateinit var mapData : TMapData
    lateinit var category : String


    lateinit var arrayPOI : ArrayList<POI>
    companion object{
        class POI{
            var name : String? = null
            var latitude : Double? = null
            var longitute : Double? = null
        }
    }

    //Marker
    private var markerIdList = ArrayList<String>()

    //PopupList Item List
    //private var popupListItems = ArrayList<PopupListItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        mapView = TMapView(this)
        Toast.makeText(this, "실제 위치와 50M 정도 차이날 수 있습니다", Toast.LENGTH_SHORT).show()

        try {
            //gps = TMapGpsManager(this)
            initView()

            mapData = TMapData()
            checkPermission()

            GpsManager.init(this)
            gpsManager = GpsManager.getInstance()
            gpsManager.setOnLocationListener(locationListener)
            pathManager = PathManager.getInstance()
            directionManager = DirectionManager().getInstance()



            arrayPOI = ArrayList<POI>()
            adapter = SearchListAdapter()
            popup.adapter = adapter

            btnAR.setOnClickListener{
                val intent = Intent(this, ARActivity::class.java)
                startActivityForResult(intent, REQUEST_AR)
            }

           connectserver.setOnClickListener {
               val connetion = ConnectServer(
                   File("/mnt/sdcard/DCIM/Camera/20180729_194225_HDR.jpg"),
                   object : ConnectServer.EventListener {
                       override fun onSocketResult(result: String) {
                           toolbar.setTitle(result + "을 찾아라!")
                           category = result

                       }

                       override fun onSocketFailed() {
                           val builder = AlertDialog.Builder(this@MainActivity)
                               .setTitle("안내")
                               .setMessage("실패")
                               .setPositiveButton("확인", null)
                               .show()
                       }
                   })
               connetion.start()
           }

            popup.setOnItemClickListener(object : AdapterView.OnItemClickListener{
                override fun onItemClick(
                    parent: AdapterView<*>?,
                    view: View?,
                    index : Int,
                    id: Long
                ) {
                    try{
                        if (index >= arrayPOI.size) {
                            return
                        }

                        val builder = AlertDialog.Builder(this@MainActivity)
                        builder.setTitle("안내")
                            .setMessage("${arrayPOI.get(index).name}를 출발지로 다시 설정하시겠습니까?")
                            .setNegativeButton("아니오", null)
                            .setPositiveButton("예", object : DialogInterface.OnClickListener{
                                override fun onClick(dialog: DialogInterface?, which: Int) {

                                    Restart_Point = TMapPoint(arrayPOI.get(index).latitude!!, arrayPOI.get(index).longitute!!)
                                    if(Restart_Point != null){
                                        toolbar.setTitle("출발지 : "+arrayPOI.get(index).name)
                                    }
                                    popup.visibility = View.GONE
                                    aroundcontroller = 0


                                    if(destination != null){
                                        setNavigationMode(true)
                                    }

                                }
                            }).show()
                    } catch (e : Exception){
                        Log.d("Exception:" , e.message)
                    }
                }
            })

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
        toolbar.setTitle("반갑습니다!")
        toolbar.setTitleTextColor(resources.getColor(R.color.colorBlack))

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
        btnAR.visibility = View.GONE

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
                Now_Point = TMapPoint(currentLocation.latitude, currentLocation.longitude)
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
            btnAR.visibility = View.GONE

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
                btnAR.visibility = View.VISIBLE
                toolbar.setTitle("주변을 조심하세요!")


                val currentLocation = gpsManager.currentLocation
                if (currentLocation != null) {
                    mapView.setLocationPoint(currentLocation.longitude, currentLocation.latitude)
                    mapView.setCenterPoint(currentLocation.longitude, currentLocation.latitude)
                }

                val startPoint = TMapPoint(currentLocation!!.latitude, currentLocation.longitude)

                val endpoint = destination
                //getRoute(endpoint!!)

                if(Restart_Point == null){
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
                                    //Log.d("now", "${now}")
                                    val expectedtime : Long = now + (ParseJson.totalTime!!.toLong()*1000L)
                                    //Log.d("et", "${expectedtime}")
                                    val date1 =Date(expectedtime)
                                    val sdfNow = SimpleDateFormat("HH:MM")
                                    val formatDate = sdfNow.format(date1)
                                    totaltime.text = "${formatDate} 도착예정"


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
                } else {
                    val routeApi =
                        RouteApi(this, Restart_Point!!, endpoint!!, object : RouteApi.EventListener {
                            override fun onApiResult(jsonString: String) {
                                Toast.makeText(this@MainActivity, "요청성공", Toast.LENGTH_SHORT).show()
                                try {
                                    val objects: JSONObject = JSONObject(jsonString)
                                    Log.d("result:", objects.toString())

                                    val polyLine = parseJSON(objects)
                                    Log.d("totaltime","${ParseJson.totalTime}")

                                    val now = System.currentTimeMillis()
                                    //Log.d("now", "${now}")
                                    val expectedtime : Long = now + (ParseJson.totalTime!!.toLong()*1000L)
                                    //Log.d("et", "${expectedtime}")
                                    val date1 =Date(expectedtime)
                                    val sdfNow = SimpleDateFormat("HH:MM")
                                    val formatDate = sdfNow.format(date1)
                                    totaltime.text = "${formatDate} 도착예정"


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
                    Restart_Point = null
                }



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

   /* private fun getRoute(endPoint: TMapPoint){

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
    }*/

    private fun setDestination(destination : TMapPoint) {
        //clear previous destination
        try{
            mapView.removeMarkerItem("도착지")
        } catch (ex : Exception){

        }

        this.destination = destination

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
        } else if(aroundcontroller == 1){
            popup.visibility = View.GONE
            aroundcontroller = 0
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
                        Log.d("시발", "여기오냐")

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
                REQUEST_AR -> {
                    moveToCurrentLocation()
                    val poi = data?.getStringExtra("POI")
                    val lat = data?.getDoubleExtra("LAT", 0.0)
                    val lon = data?.getDoubleExtra("LON", 0.0)

                    if(lat != null && lon != null) {
                        val mapPoint = TMapPoint(lat, lon)
                        setDestination(mapPoint)
                        mapView.setCenterPoint(lon, lat)
                    }

                    if (Now_Point == null) {
                        Toast.makeText(this@MainActivity, "내 위치가 설정되지 않았습니다." ,Toast.LENGTH_SHORT )
                    } else {
                        try {
                            mapData.findAroundNamePOI(
                                Now_Point,
                                poi,
                                object : TMapData.FindAroundNamePOIListenerCallback {
                                    override fun onFindAroundNamePOI(arrayList: java.util.ArrayList<TMapPOIItem>) {
                                        runOnUiThread(object : Runnable {
                                            override fun run() {
                                                adapter.clear()
                                                arrayPOI.clear()

                                                Restart_Point = TMapPoint(arrayList.get(0).poiPoint.latitude, arrayList.get(0).poiPoint.longitude)

                                                for (i in 0 until arrayList.size) {
                                                    var poiItem: TMapPOIItem = arrayList.get(i)

                                                    Log.d("아이템", "${arrayList.get(i)}")
                                                    val distance =
                                                        poiItem.getDistance(Now_Point)
                                                            .toInt()

                                                    adapter.addItem(
                                                        poiItem.poiName,
                                                        "현재 위치로 부터 ${distance}m 떨어져 있습니다."
                                                    )


                                                    val poi = POI()
                                                    poi.name = poiItem.poiName
                                                    poi.latitude = poiItem.poiPoint.latitude
                                                    poi.longitute = poiItem.poiPoint.longitude

                                                    arrayPOI.add(poi)
                                                }
                                                adapter.notifyDataSetChanged()
                                                //popup.visibility = View.VISIBLE
                                                //aroundcontroller = 1

                                                if(Restart_Point != null){
                                                    setNavigationMode(true)
                                                }

                                            }
                                        })
                                    }
                                })
                        } catch (e: Exception) {
                            Log.d("Exception", e.message)
                        }
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
                    Log.d("좌표", "${Now_Point}")
                    val intent = Intent(this , SearchActivity::class.java)

                        intent.putExtra("lat", Now_Point!!.latitude)
                        intent.putExtra("lon", Now_Point!!.longitude)

                    startActivityForResult(intent, REQUEST_SEARCH)
                }
                R.id.nav_history -> {
                    val intent = Intent(this, HistoryActivity::class.java)
                    startActivityForResult(intent, REQUEST_HISTORY)
                }
                R.id.nav_around -> {
                    val intent = Intent(this, AroundActivity::class.java)
                    startActivityForResult(intent, REQUEST_AROUND)
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
            Log.d("버퍼", "${bw}")
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
                        //mapView.setCenterPoint(location.longitude, location.latitude)
                        if(navigationMode){
                            moveToCurrentLocation()
                            //updateDirection()
                        }
                    }
                }

                Log.d("테스트", location.toString())

            } catch (e : Exception){ }
        }

        override fun onProviderDisabled(provider: String?) {
            Log.d("Provider변경", provider)
        }

        override fun onProviderEnabled(provider: String?) {
            Log.d("Provider변경", provider)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.d("Provider변경", provider)
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
            if (Now_Point == null) {
                Toast.makeText(this@MainActivity, "내 위치가 설정되지 않았습니다." ,Toast.LENGTH_SHORT )
            } else {
                try {
                    mapData.findAroundNamePOI(
                        Now_Point,
                        category,
                        object : TMapData.FindAroundNamePOIListenerCallback {
                            override fun onFindAroundNamePOI(arrayList: java.util.ArrayList<TMapPOIItem>) {
                                runOnUiThread(object : Runnable {
                                    override fun run() {
                                        adapter.clear()
                                        arrayPOI.clear()

                                        for (i in 0 until arrayList.size) {
                                            var poiItem: TMapPOIItem = arrayList.get(i)

                                            Log.d("아이템", "${arrayList.get(i)}")
                                            val distance =
                                                poiItem.getDistance(Now_Point)
                                                    .toInt()

                                            adapter.addItem(
                                                poiItem.poiName,
                                                "현재 위치로 부터 ${distance}m 떨어져 있습니다."
                                            )

                                            val poi = POI()
                                            poi.name = poiItem.poiName
                                            poi.latitude = poiItem.poiPoint.latitude
                                            poi.longitute = poiItem.poiPoint.longitude

                                            arrayPOI.add(poi)
                                        }
                                        adapter.notifyDataSetChanged()
                                        popup.visibility = View.VISIBLE
                                        aroundcontroller = 1
                                    }
                                })
                            }
                        })
                } catch (e: Exception) {
                    Log.d("Exception", e.message)
                }

            }
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
