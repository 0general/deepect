package kr.ac.inu.deepect.arnavigation.navigation

import android.graphics.Bitmap
import android.os.StrictMode
import android.util.Base64
import android.util.Log
import com.google.flatbuffers.Utf8
import java.io.*
import java.net.Socket
import java.net.UnknownHostException
import java.util.logging.SocketHandler


class ConnectServer(private val file : File) : Thread() {


    private lateinit var clientSocket : Socket
    private lateinit var socketIn : BufferedReader
    private lateinit var socketOut : PrintWriter
    private lateinit var dis : DataInputStream
    private lateinit var dos : DataOutputStream
    private val port = 5000
    private val ip = "3.12.250.104"
    private lateinit var mHandler: SocketHandler
    private lateinit var mThread: Thread


    /*fun getBase64String(bitmap : Bitmap) : String{
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val imageByte = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(imageByte,Base64.NO_WRAP)
    }

    fun makeFileWithString(base64 : String) {
        val decode : ByteArray = Base64.decode(base64, Base64.DEFAULT)
        val fos : FileOutputStream
        try {
            val target : File = File("/mnt/sdcard/DCIM/Camera/1")
            target.createNewFile()
            fos = FileOutputStream(target)
            fos.write(decode)
            fos.close()

        } catch (e : java.lang.Exception){
            e.printStackTrace()
        }
    }

    fun toBase64String(file: File) : String {
        var encodedstr : String = ""
        val fis : FileInputStream
        try {
            val bArr : ByteArray = ByteArray(file.length().toInt() -1)
            fis = FileInputStream(file)
            fis.read(bArr,0, bArr.size -1)
            fis.close()
            encodedstr = Base64.encodeToString(bArr,Base64.NO_WRAP)
            Log.d("encodestr", encodedstr)
        } catch (e : java.lang.Exception) {
            e.printStackTrace()
        }
        return encodedstr
    }*/

    fun getFileSize() : Int{
        var size = ""
        var filesize : Long = 0
        val file = File("/mnt/sdcard/DCIM/Camera/20180729_194225_HDR.jpg")
        if(file.exists()){
            filesize = file.length()
        }
        Log.d("filesize", "${filesize}")
        return filesize.toInt()
    }

    override fun run() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        //getFileSize()

        try { // 소켓을 생성하고 입출력 스트립을 소켓에 연결
            clientSocket = Socket(ip , port)
            Log.d("Socket>>>>>>", "ip and port open Success!!!!!")
            //val inputStream = clientSocket.getInputStream()
            val tempfile = file

            try{
                //socketIn = BufferedReader(InputStreamReader(clientSocket.getInputStream(), "UTF-8"))
                socketOut = PrintWriter(BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream())),true)
                dis = DataInputStream(FileInputStream(tempfile))
                dos = DataOutputStream(clientSocket.getOutputStream())
                val buf = ByteArray(1024)
                while (dis.read(buf) > 0) {
                    dos.write(buf)
                    dos.flush()
                }
                dos.close()

            } catch (e : Exception){
                Log.d("error", "${e}")
            } finally {
                clientSocket.close()
            }

        } catch (uhe : UnknownHostException){
            Log.e("error", "생성 Error : 호스트의 IP 주소 식별 불가")
        } catch (ioe : IOException) {
            Log.e("error", "생성 Error : 네트워크 응답 x")
        } catch (se : SecurityException){
            Log.e("error" , "생성 Error : 보안위반")
        } catch( le : IllegalArgumentException) {
            Log.e("error", "생성 Error : 잘못된 파라미터 전달")
        }
    }

}