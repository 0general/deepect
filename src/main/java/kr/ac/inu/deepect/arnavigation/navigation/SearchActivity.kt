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
import kotlinx.android.synthetic.main.activity_search.*
import kr.ac.inu.deepect.R
import kr.ac.inu.deepect.arnavigation.navigation.utils.StringUtils

class SearchActivity : AppCompatActivity() {


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
        setContentView(R.layout.activity_search)

        mapData = TMapData()

        val adapter = SearchListAdapter()
        arrayPOI = ArrayList<POI>()
        lvSearch.adapter = adapter

        lvSearch.setOnItemClickListener(object : AdapterView.OnItemClickListener{
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

                    val builder = AlertDialog.Builder(this@SearchActivity)
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

        btnSearch.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                try {
                    mapData.findAllPOI(editSearch.text.toString(), 20, object : TMapData.FindAllPOIListenerCallback{
                        override fun onFindAllPOI(arrayList : java.util.ArrayList<TMapPOIItem>) {
                            runOnUiThread(object : Runnable {
                                override fun run() {
                                    adapter.clear()
                                    arrayPOI.clear()

                                    for(i in 0 until arrayList.size){
                                        var poiItem : TMapPOIItem = arrayList.get(i)
                                        val secondLine = StringUtils.join(
                                                '/',
                                                arrayOf(
                                                        poiItem.upperBizName,
                                                        poiItem.middleBizName,
                                                        poiItem.lowerBizName
                                                        //poiItem.detailBizName
                                                )
                                        )
                                        adapter.addItem(poiItem.poiName, secondLine)


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
                } catch (e : Exception){
                    Log.d("Exception", e.message)
                }
            }
        })

    }
}