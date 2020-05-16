package kr.ac.inu.deepect.arnavigation.navigation

import android.content.Context
import android.os.Looper
import com.skt.Tmap.TMapPoint
import kr.ac.inu.deepect.R
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class RouteApi(context : Context, startPoint : TMapPoint, endPoint: TMapPoint, eventListener: EventListener) : Thread() {

    interface EventListener {
        fun onApiResult(result : String)
        fun onApiFailed()
    }

    private var handler : android.os.Handler
    private var listener : EventListener?
    private var apiKey : String
    private var startPoint : TMapPoint
    private var endPoint : TMapPoint

    init {
        this.handler = android.os.Handler(Looper.getMainLooper())
        this.listener = eventListener
        this.apiKey = context.resources.getString(R.string.tmap_api_key)
        this.startPoint = startPoint
        this.endPoint = endPoint

    }

    override fun run() {
        var httpsURLConnection : HttpsURLConnection? = null

        try {
            val BASE = "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1&format=json&appKey=%s&startX=%s&startY=%s&endX=%s&endY=%s&startName=출발점&endName=도착점&reqCoordType=WGS84GEO&resCoordType=WGS84GEO"

            val url = URL(
                    java.lang.String.format(
                            BASE,
                            apiKey,
                            startPoint.longitude,
                            startPoint.latitude,
                            endPoint.longitude,
                            endPoint.latitude
                    )
            )

            httpsURLConnection = url.openConnection() as HttpsURLConnection

            if(httpsURLConnection.responseCode == HttpURLConnection.HTTP_OK){
                var inputStream : InputStream = httpsURLConnection.inputStream

                var inputStreamReader = InputStreamReader(inputStream, "UTF-8")

                var bufferedReader = BufferedReader(inputStreamReader)

                var line : String?

                var stringBuilder = StringBuilder()

                do {
                    line = bufferedReader.readLine()
                    if(line == null)
                        break
                    stringBuilder.append(line)
                } while ( line != null)

                bufferedReader.close()

                var result = stringBuilder.toString()

                onApiResult(result)
            } else {
                onApiFailed()
            }
        } catch (e : Exception) {
            e.printStackTrace()
            onApiFailed()
        } finally {
            if(httpsURLConnection != null){
                httpsURLConnection.disconnect()
            }
        }
    }

    private  fun onApiResult(result: String){
        if(listener != null){
            handler.post(object : Runnable{
                override fun run() {
                    listener?.onApiResult(result)
                }
            })
        }
    }

    private fun onApiFailed() {
        if(listener != null) {
            handler.post(object : Runnable{
                override fun run() {
                    listener?.onApiFailed()
                }
            })
        }
    }

}
