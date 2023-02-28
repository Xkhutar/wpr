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
import java.net.ServerSocket

class NetworkSus : WifiP2pManager.ConnectionInfoListener {

    private var device: WifiP2pDevice? = null
    private var info: WifiP2pInfo? = null
    private var activity: AppCompatActivity? = null

    constructor(activity: AppCompatActivity) {
        this.activity = activity
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
        this.info = info;
        if (info!!.groupFormed && info!!.isGroupOwner) {
            FileServerAsyncTask(activity!!.applicationContext).execute()
        } else if (info!!.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.
            println("I AM CLIENT!")
        }
    }

    class FileServerAsyncTask(private val context: Context) : AsyncTask<Void?, Void?, String?>() {
        /**
         * @param context
         * @param statusText
         */

        protected override fun doInBackground(vararg p0: Void?): String? {
            try {
                val serverSocket = ServerSocket(8988)
                Log.d("FSTASK", "Server: Socket opened")
                val client = serverSocket.accept()
                Log.d("FSTASK", "Server: connection done")
                val f = File(
                    context.getExternalFilesDir("received"),
                    ("wifip2pshared-" + System.currentTimeMillis()
                            + ".jpg")
                )
                val dirs = File(f.parent)
                if (!dirs.exists()) dirs.mkdirs()
                f.createNewFile()
                Log.d("FSTASK", "server: copying files $f")
                val inputstream = client.getInputStream()
                copyFile(inputstream, FileOutputStream(f))
                serverSocket.close()
                return f.absolutePath
            } catch (e: IOException) {
                Log.e("FSTASK", (e.message)!!)
                return "null"
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        protected override fun onPostExecute(result: String?) {
            if (result != null) {
                val recvFile = File(result)
                val fileUri: Uri = FileProvider.getUriForFile(
                    context,
                    "com.example.android.wifidirect.fileprovider",
                    recvFile
                )
                val intent = Intent()
                intent.setAction(Intent.ACTION_VIEW)
                intent.setDataAndType(fileUri, "image/*")
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(intent)
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