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
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class NetworkAmongus {

    private var device: WifiP2pDevice? = null
    private var info: WifiP2pInfo? = null
    private var activity: AppCompatActivity? = null


    constructor(activity: AppCompatActivity) {
        this.activity = activity
    }

    class ClientHandshake(private val port: Int, private val hostAddress: InetAddress, private val onTerminate: () -> Unit) : AsyncTask<Void?, Void?, String?>()
    {
        protected override fun doInBackground(vararg p0: Void?): String? {
            Log.d("HANDSHAKE","CLIENT")
            val socket = Socket()
            socket.reuseAddress = true
            socket.bind(null)
            socket.connect(InetSocketAddress(hostAddress, port), 10000)
            socket.close()
            return "YIPEE"
        }

        protected override fun onPostExecute(result: String?) {
            onTerminate()
        }
    }

    class FileClientAsyncTask(private val context: Context, private val path: String, private val hostAddress: InetAddress, private val commonPort: Int) : AsyncTask<Void?, Void?, String?>() {
        /**
         * @param context
         * @param statusText
         */

        protected override fun doInBackground(vararg p0: Void?): String? {
            Log.d(TAG, "YOU TRIED LMAO!")
            val file = File(path) //File(getFilesDir().toString() + "/recording.mp3")
            val fileUri = Uri.fromFile(file)
            val socket = Socket()

            try {
                Log.d("", "Opening client socket - "+hostAddress.toString() +":"+commonPort)
                socket.reuseAddress = true
                socket.bind(null)
                socket.connect(InetSocketAddress(hostAddress, commonPort), 10000)

                Log.d(TAG, "Client socket - " + socket.isConnected)
                val outputStream = socket.getOutputStream()
                val cr = context.contentResolver
                var inputStream: InputStream? = null
                try {
                    inputStream = cr.openInputStream(fileUri)
                } catch (e: FileNotFoundException) {
                    Log.d(TAG, e.message!!)
                }

                copyFile(inputStream!!, outputStream)

                Log.d(TAG, "Client: Data written")
            } catch (e: IOException) {
                Log.e(TAG, e.stackTraceToString())
            } finally {
                if (socket != null) {
                    if (socket.isConnected) {
                        try {
                            socket.close()
                        } catch (e: IOException) {
                            // Give up
                            e.printStackTrace()
                        }

                    }
                }
            }

            return "YIPEE";
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        protected override fun onPostExecute(result: String?) {
            if (result != null) {
                Log.d("FSTASK", "YIPEE!")
            }
        }



    }

    companion object {
        private const val TAG = "CLTASK";
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