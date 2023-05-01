package com.example.bprac

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
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

public class NetworkSus {
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
            onTerminate()
        }
    }

    class FileServerAsyncTask(private val port: Int) : Runnable {

        override fun run() {
            while (true) {
                Log.d(TAG, "Audio receiver started.")
                try {
                    val buffer = ByteArray(PACKET_SIZE)
                    val serverSocket = ServerSocket(port)
                    val client = serverSocket.accept()
                    val inputStream = client.getInputStream()
                    val audioPlayer = AudioTrack(
                        AudioManager.STREAM_SYSTEM,
                        8192,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        PACKET_SIZE*10,
                        AudioTrack.MODE_STREAM
                    )

                    var bytesRead = 0

                    while (true) {

                        var numberBytes = inputStream.read(buffer, bytesRead, PACKET_SIZE - bytesRead)
                        if (numberBytes > 0) {
                            if (bytesRead + numberBytes == PACKET_SIZE) {
                                audioPlayer!!.write(buffer, 0, PACKET_SIZE)
                                bytesRead = 0
                                numberBytes = 0
                                audioPlayer!!.play()
                            }

                            bytesRead += numberBytes
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, (e.message)!!)
                }
            }
        }
    }

    companion object {
        private const val TAG = "S-TASK"
        public const val PACKET_SIZE = 128;
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