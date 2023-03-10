package com.example.bprac

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.FileProvider
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket

class NetworkSus {

    private var device: WifiP2pDevice? = null
    private var info: WifiP2pInfo? = null
    private var activity: AppCompatActivity? = null

    constructor(activity: AppCompatActivity) {
        this.activity = activity
    }

    class ServerHandshake(private val port: Int, private val receptor: (InetAddress) -> Unit, private val onTerminate: () -> Unit) : AsyncTask<Void?, Void?, String?>()
    {
        protected override fun doInBackground(vararg p0: Void?): String? {
            Log.d("HANDSHAKE","SERVER")
            val serverSocket = ServerSocket(port)
            val client = serverSocket.accept()
            receptor(client.inetAddress);
            serverSocket.close()
            return "YIPEE"
        }

        protected override fun onPostExecute(result: String?) {
            Log.d("123", "YIPEE!")
            onTerminate()
        }
    }

    class FileServerAsyncTask(private val path: String, private val port: Int, private val onFinish: () -> Unit) : Runnable {

        override fun run() {
            while (true) {
                Log.d("CHILLING", "LOL")
                try {
                    val serverSocket = ServerSocket(port)
                    Log.d("FSTASK", "Server: Socket opened")
                    val client = serverSocket.accept()
                    Log.d("FSTASK", "Server: connection done")
                    val file = File(path)
                    if (file.exists()) file.delete()
                    file.createNewFile()
                    Log.d("FSTASK", "server: copying files $file")
                    val inputstream = client.getInputStream()
                    copyFile(inputstream, FileOutputStream(file))
                    serverSocket.close()
                    onFinish()
                } catch (e: IOException) {
                    Log.e("FSTASK", (e.message)!!)
                }
            }
        }
    }

    companion object {
        protected val CHOOSE_FILE_RESULT_CODE = 20
        fun copyFile(inputStream: InputStream, out: OutputStream): Boolean {
            val buf = ByteArray(1024)
            var len: Int
            try {
                while ((inputStream.read(buf).also { len = it }) != -1) {
                    out.write(buf, 0, len)
                }
                out.close()
                inputStream.close()
            } catch (e: IOException) {
                Log.d("FSTASK", e.toString())
                return false
            }
            return true
        }
    }
}