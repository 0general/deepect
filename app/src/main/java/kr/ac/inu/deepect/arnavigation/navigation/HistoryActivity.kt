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
import kotlinx.android.synthetic.main.activity_history.*
import kr.ac.inu.deepect.R
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class HistoryActivity : AppCompatActivity() {

    lateinit var arrayPOI : ArrayList<SearchActivity.Companion.POI>
    lateinit var adapter : SearchListAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        arrayPOI = ArrayList<SearchActivity.Companion.POI>()
        adapter = SearchListAdapter()

        btnErase.setOnClickListener {
            try {
                val file = File(filesDir , "history.txt")
                if(file.exists()){
                    file.delete()
                }

            } catch (e : Exception){
                Log.d("Erase", e.message)
            }
        }

        lvHistory.adapter = adapter

        try {
            val file = File(filesDir , "history.txt")
            val br = BufferedReader(FileReader(file))

            var line : String? = null

            while (br.readLine().also { line = it } != null) {
                val vals = line!!.split(" ").toTypedArray()
                val name = vals[0]
                val longitude = java.lang.Double.valueOf(vals[1])
                val latitude = java.lang.Double.valueOf(vals[2])
                val poi = SearchActivity.Companion.POI()
                poi.name = name
                poi.longitute = longitude
                poi.latitude = latitude
                arrayPOI.add(poi)
                adapter.addItem(name, "위도:$longitude/경도:$latitude")
            }

            adapter.notifyDataSetChanged()
            br.close()
        } catch (e: Exception){

        }

        lvHistory.setOnItemClickListener(object : AdapterView.OnItemClickListener{
            override fun onItemClick(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
            ) {
                try {
                    if (position >= arrayPOI.size) return

                    val builder = AlertDialog.Builder(this@HistoryActivity)
                    builder.setTitle("안내")
                            .setMessage("${arrayPOI.get(position).name}를 도착지로 설정하시겠습니까?")
                            .setNegativeButton("아니오", null)
                            .setPositiveButton("예", object : DialogInterface.OnClickListener{
                                override fun onClick(dialog: DialogInterface?, which: Int) {
                                    val intent = Intent()
                                    intent.putExtra("POI", arrayPOI.get(position).name)
                                    intent.putExtra("LON", arrayPOI.get(position).longitute)
                                    intent.putExtra("LAT", arrayPOI.get(position).latitude)

                                    setResult(Activity.RESULT_OK, intent)
                                    finish()
                                }
                            }).show()
                } catch (e : Exception){
                    Log.d("Exception", e.message)
                }
            }
        })
    }
}