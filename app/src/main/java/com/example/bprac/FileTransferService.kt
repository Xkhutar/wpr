package com.example.bprac

import android.app.IntentService
import android.content.Intent
import com.example.bprac.FileTransferService
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket

class FileTransferService : IntentService {
    constructor(name: String?) : super(name) {}
    constructor() : super("FileTransferService") {}

    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    override fun onHandleIntent(intent: Intent?) {
        val context = applicationContext
        if (intent!!.action == ACTION_SEND_FILE) {
            val fileUri = intent.extras!!.getString(EXTRAS_FILE_PATH)
            val host = intent.extras!!.getString(EXTRAS_GROUP_OWNER_ADDRESS)
            val socket = Socket()
            val port = intent.extras!!.getInt(EXTRAS_GROUP_OWNER_PORT)
            try {
                Log.d(WifiDirectAction.TAG, "Opening client socket - ")
                socket.bind(null)
                socket.connect(InetSocketAddress(host, port), SOCKET_TIMEOUT)
                Log.d(WifiDirectAction.TAG, "Client socket - " + socket.isConnected)
                val stream = socket.getOutputStream()
                val cr = context.contentResolver
                var `is`: InputStream? = null
                try {
                    `is` = cr.openInputStream(Uri.parse(fileUri))
                } catch (e: FileNotFoundException) {
                    Log.d(WifiDirectAction.TAG, e.toString())
                }
                if (`is` != null) {
                    DeviceDetailFragment.copyFile(`is`, stream)
                }
                Log.d(WifiDirectAction.TAG, "Client: Data written")
            } catch (e: IOException) {
                Log.e(WifiDirectAction.TAG, e.message!!)
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
        }
    }

    companion object {
        private const val SOCKET_TIMEOUT = 5000
        const val ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE"
        const val EXTRAS_FILE_PATH = "file_url"
        const val EXTRAS_GROUP_OWNER_ADDRESS = "go_host"
        const val EXTRAS_GROUP_OWNER_PORT = "go_port"
    }
}