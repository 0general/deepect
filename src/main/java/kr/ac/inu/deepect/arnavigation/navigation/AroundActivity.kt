package kr.ac.inu.deepect.arnavigation.navigation

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.skt.Tmap.TMapData
import com.skt.Tmap.TMapPOIItem
import com.skt.Tmap.TMapPoint
import kotlinx.android.synthetic.main.activity_around.*
import kotlinx.android.synthetic.main.activity_search.*
import kr.ac.inu.deepect.R
import kr.ac.inu.deepect.arnavigation.navigation.utils.StringUtils

class AroundActivity : AppCompatActivity() {


    lateinit var mapData : TMapData


    lateinit var arrayPOI : ArrayList<POI>
    lateinit var adapter: SearchListAdapter

    companion object{
        class POI{
            var name : String? = null
            var latitude : Double? = null
            var longitute : Double? = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_around)

        mapData = TMapData()

        val adapter = SearchListAdapter()
        arrayPOI = ArrayList<POI>()

        lvSearch_around.adapter = adapter

        lvSearch_around.setOnItemClickListener(object : AdapterView.OnItemClickListener{
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

                    val builder = AlertDialog.Builder(this@AroundActivity)
                    builder.setTitle("안내")
                            .setMessage("${arrayPOI.get(index).name}를 도착지로 설정하시겠습니까?")
                            .setNegativeButton("아니오", null)
                            .setPositiveButton("예", object : DialogInterface.OnClickListener{
                                override fun onClick(dialog: DialogInterface?, which: Int) {
                                    val intent = Intent().apply {
                                        putExtra("POI", arrayPOI.get(index).name)
                                        putExtra("LON", arrayPOI.get(index).longitute)
                                        putExtra("LAT", arrayPOI.get(index).latitude)
                                    }
                                    setResult(Activity.RESULT_OK, intent)
                                    finish()
                                }
                            }).show()
                } catch (e : Exception){
                    Log.d("Exception:" , e.message)
                }
            }
        })

        btnSearch_around.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                try{
                    mapData.findAroundNamePOI(TMapPoint(37.37748,126.65333), "lotteria", object : TMapData.FindAroundNamePOIListenerCallback {
                        override fun onFindAroundNamePOI(arrayList: java.util.ArrayList<TMapPOIItem>) {
                            runOnUiThread(object : Runnable {
                                override fun run() {
                                    adapter.clear()
                                    arrayPOI.clear()

                                    for(i in 0 until arrayList.size){
                                        var poiItem : TMapPOIItem = arrayList.get(i)

                                        Log.d("아이템", "${arrayList.get(i)}")
                                        val distance = poiItem.getDistance(TMapPoint(37.37748,126.65333)).toInt()
                                        /*val secondLine = StringUtils.join('/', arrayOf(
                                            poiItem.upperBizName,
                                            poiItem.middleBizName,
                                            poiItem.lowerBizName
                                        ))*/

                                        val secondLine = StringUtils.join('/', arrayOf(
                                                "1","2","3"
                                        ))
                                        adapter.addItem(poiItem.poiName, "현재 위치로 부터 ${distance} 떨어져 있습니다.")

                                        val poi = POI()
                                        poi.name = poiItem.poiName
                                        poi.latitude = poiItem.poiPoint.latitude
                                        poi.longitute = poiItem.poiPoint.longitude

                                        arrayPOI.add(poi)
                                    }
                                    adapter.notifyDataSetChanged()
                                }
                            })
                        }
                    })
                }catch (e : Exception){
                    Log.d("Exception", e.message)
                }

            }
        })

    }
}