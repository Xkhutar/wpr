package com.example.bprac

import android.app.Fragment
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.support.v4.content.FileProvider
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.FileProvider
import java.io.*
import java.net.ServerSocket

class DeviceDetailFragment() : Fragment(), WifiP2pManager.ConnectionInfoListener {
    private var mContentView: View? = null
    private var device: WifiP2pDevice? = null
    private var info: WifiP2pInfo? = null
    var progressDialog: ProgressDialog? = null
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle
    ): View? {
        mContentView = inflater.inflate(R.layout.device_layout, null)
        mContentView!!.findViewById<View>(R.id.btn_connect)
            .setOnClickListener(View.OnClickListener {
                val config = WifiP2pConfig()
                config.deviceAddress = device?.deviceAddress
                config.wps.setup = WpsInfo.PBC
                if (progressDialog != null && progressDialog!!.isShowing()) {
                    progressDialog!!.dismiss()
                }
                progressDialog = ProgressDialog.show(
                    activity,
                    "Press back to cancel",
                    "Connecting to :" + device?.deviceAddress,
                    true,
                    true //                        new DialogInterface.OnCancelListener() {
                    //
                    //                            @Override
                    //                            public void onCancel(DialogInterface dialog) {
                    //                                ((DeviceActionListener) getActivity()).cancelDisconnect();
                    //                            }
                    //                        }
                )
                (activity as DeviceActionListener).connect(config)
            })
        mContentView!!.findViewById<View>(R.id.btn_disconnect).setOnClickListener(
            object : View.OnClickListener {
                override fun onClick(v: View) {
                    (activity as DeviceActionListener).disconnect()
                }
            })
        mContentView!!.findViewById<View>(R.id.btn_start_client).setOnClickListener(
            object : View.OnClickListener {
                override fun onClick(v: View) {
                    // Allow user to pick an image from Gallery or other
                    // registered apps
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "image/*"
                    startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE)
                }
            })
        return mContentView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        val uri: Uri? = data.data
        val statusText: TextView = mContentView!!.findViewById<View>(R.id.status_text) as TextView
        statusText.setText("Sending: $uri")
        Log.d(WifiDirectAction.TAG, "Intent----------- $uri")
        val serviceIntent = Intent(activity, FileTransferService::class.java)
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE)
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString())
        serviceIntent.putExtra(
            FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
            info.groupOwnerAddress.getHostAddress()
        )
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988)
        activity.startService(serviceIntent)
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        if (progressDialog != null && progressDialog!!.isShowing()) {
            progressDialog!!.dismiss()
        }
        this.info = info
        this.view!!.visibility = View.VISIBLE
        // The owner IP is now known.
        var view: TextView = mContentView!!.findViewById<View>(R.id.group_owner) as TextView
        view.setText(
            (resources.getString(R.string.group_owner_text)
                    + (if ((info.isGroupOwner == true)) resources.getString(R.string.yes) else resources.getString(
                R.string.no
            )))
        )
        // InetAddress from WifiP2pInfo struct.
        view = mContentView!!.findViewById<View>(R.id.device_info) as TextView
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress())
        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {
            FileServerAsyncTask(activity, mContentView!!.findViewById(R.id.status_text))
                .execute()
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.
            mContentView!!.findViewById<View>(R.id.btn_start_client).visibility =
                View.VISIBLE
            (mContentView!!.findViewById<View>(R.id.status_text) as TextView).setText(
                resources
                    .getString(R.string.client_text)
            )
        }
        // hide the connect button
        mContentView!!.findViewById<View>(R.id.btn_connect).visibility = View.GONE
    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    fun showDetails(device: WifiP2pDevice) {
        this.device = device
        this.view!!.visibility = View.VISIBLE
        var view: TextView = mContentView!!.findViewById<View>(R.id.device_address) as TextView
        view.setText(device.deviceAddress)
        view = mContentView!!.findViewById<View>(R.id.device_info) as TextView
        view.setText(device.toString())
    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    fun resetViews() {
        mContentView!!.findViewById<View>(R.id.btn_connect).visibility = View.VISIBLE
        var view: TextView = mContentView!!.findViewById<View>(R.id.device_address) as TextView
        view.setText(R.string.empty)
        view = mContentView!!.findViewById<View>(R.id.device_info) as TextView
        view.setText(R.string.empty)
        view = mContentView!!.findViewById<View>(R.id.group_owner) as TextView
        view.setText(R.string.empty)
        view = mContentView!!.findViewById<View>(R.id.status_text) as TextView
        view.setText(R.string.empty)
        mContentView!!.findViewById<View>(R.id.btn_start_client).visibility = View.GONE
        view!!.visibility = View.GONE
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    class FileServerAsyncTask(private val context: Context, statusText: View) :
        AsyncTask<Void?, Void?, String?>() {
        private val statusText: TextView

        /**
         * @param context
         * @param statusText
         */
        init {
            this.statusText = statusText as TextView
        }

        protected override fun doInBackground(vararg params: Void): String {
            try {
                val serverSocket = ServerSocket(8988)
                Log.d(WifiDirectAction.TAG, "Server: Socket opened")
                val client = serverSocket.accept()
                Log.d(WifiDirectAction.TAG, "Server: connection done")
                val f = File(
                    context.getExternalFilesDir("received"),
                    ("wifip2pshared-" + System.currentTimeMillis()
                            + ".jpg")
                )
                val dirs = File(f.parent)
                if (!dirs.exists()) dirs.mkdirs()
                f.createNewFile()
                Log.d(WifiDirectAction.TAG, "server: copying files $f")
                val inputstream = client.getInputStream()
                copyFile(inputstream, FileOutputStream(f))
                serverSocket.close()
                return f.absolutePath
            } catch (e: IOException) {
                Log.e(WifiDirectAction.TAG, (e.message)!!)
                return "null"
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        protected override fun onPostExecute(result: String) {
            if (result != null) {
                statusText.setText("File copied - $result")
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

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        protected override fun onPreExecute() {
            statusText.text = "Opening a server socket"
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
                Log.d(WifiDirectAction.TAG, e.toString())
                return false
            }
            return true
        }
    }
}