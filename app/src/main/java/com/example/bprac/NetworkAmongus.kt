package com.example.bprac

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.FileProvider
import java.io.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class NetworkAmongus {
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

    class FileClientAsyncTask(private val context: Context, private val hostAddress: InetAddress, private val commonPort: Int) : Runnable {
        @Volatile private var currentlyRecording: Boolean = false

        private var buffer: ByteArray = ByteArray(4096)

        public fun setRecording(value: Boolean) {
            currentlyRecording = value
        }
        override fun run() {
            Log.d(TAG, "Attempting to write file to remote...")
            val socket = Socket()

            try {
                Log.d("", "Opening client socket - "+hostAddress.toString() +":"+commonPort)
                socket.reuseAddress = true
                socket.bind(null)
                socket.connect(InetSocketAddress(hostAddress, commonPort), 10000)

                Log.d(TAG, "Client socket - " + socket.isConnected)
                val outputStream = socket.getOutputStream()

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    return
                }

                val audioRecorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    8192,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    4096
                )

                audioRecorder!!.startRecording()

                try {
                    while (true) {
                        if (currentlyRecording) {
                            val amountToRead = 4096
                            audioRecorder!!.read(buffer, 0, 4096)
                            outputStream.write(buffer)
                        }
                    }
                }
                catch (exception: Exception) {
                    exception.printStackTrace()
                }

            } catch (e: IOException) {
                Log.e(TAG, e.stackTraceToString())
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
                Log.d(TAG, e.toString())
                return false
            }
            return true
        }
    }
}