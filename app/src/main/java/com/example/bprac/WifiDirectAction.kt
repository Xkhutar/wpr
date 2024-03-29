package com.example.bprac

import android.Manifest
import android.app.Activity
import android.net.wifi.p2p.WifiP2pManager.ChannelListener
import android.net.wifi.p2p.WifiP2pManager
import android.content.IntentFilter
import android.content.BroadcastReceiver
import com.example.bprac.WifiDirectAction
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import com.example.bprac.R
import android.os.Build
import android.view.MenuInflater
import android.content.Intent
import android.widget.Toast
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pConfig
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityCompat

import com.example.bprac.WifiDirectBroadcastReceiver
import com.example.bprac.DeviceListFragment.DeviceActionListener

class WifiDirectAction : Activity(), ChannelListener, DeviceActionListener {
    private var manager: WifiP2pManager? = null
    private var isWifiP2pEnabled = false
    private var retryChannel = false
    private val intentFilter = IntentFilter()
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: BroadcastReceiver? = null
    val PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1001
    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    fun setIsWifiP2pEnabled(isWifiP2pEnabled: Boolean) {
        this.isWifiP2pEnabled = isWifiP2pEnabled
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION -> if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Fine location permission is not granted!")
                finish()
            }
        }
    }

    private fun initP2p(): Boolean {
        // Device capability definition check
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Log.e(TAG, "Wi-Fi Direct is not supported by this device.")
            return false
        }
        // Hardware capability check
        val wifiManager = getApplicationContext().getSystemService(WIFI_SERVICE) as WifiManager
        if (wifiManager == null) {
            Log.e(TAG, "Cannot get Wi-Fi system service.")
            return false
        }
        if (!wifiManager.isP2pSupported) {
            Log.e(TAG, "Wi-Fi Direct is not supported by the hardware or Wi-Fi is off.")
            return false
        }
        manager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        if (manager == null) {
            Log.e(TAG, "Cannot get Wi-Fi Direct system service.")
            return false
        }
        channel = manager!!.initialize(this, mainLooper, null)
        if (channel == null) {
            Log.e(TAG, "Cannot initialize Wi-Fi Direct.")
            return false
        }
        return true
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //R means resource, the res folder
        //layout is.. the layout folder
        //main is activity_main.
        setContentView(R.layout.activity_main)

        // add necessary intent values to be matched.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        if (!initP2p()) {
            finish()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                WifiDirectAction.PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION)
            // After this point you wait for callback in
            // onRequestPermissionsResult(int, String[], int[]) overridden method
        }
    }

    /** register the BroadcastReceiver with the intent values to be matched  */
    public override fun onResume() {
        super.onResume()
        receiver = WifiDirectBroadcastReceiver() //manager, channel, this  -- these are supposed to be parameters for broadcast receiver.
        registerReceiver(receiver, intentFilter)
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    fun resetData() {
        val fragmentList: DeviceListFragment = fragmentManager
            .findFragmentById(R.id.frag_list) as DeviceListFragment
        val fragmentDetails: DeviceDetailFragment = fragmentManager
            .findFragmentById(R.id.frag_detail) as DeviceDetailFragment
        if (fragmentList != null) {
            fragmentList.clearPeers()
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.action_items, menu)
        return true
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.atn_direct_enable -> {
                if (manager != null && channel != null) {
                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.
                    startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                } else {
                    Log.e(TAG, "channel or manager is null")
                }
                true
            }
            R.id.atn_direct_discover -> {
                if (!isWifiP2pEnabled) {
                    Toast.makeText(
                        this@WifiDirectAction, R.string.p2p_off_warning,
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }
                val fragment: DeviceListFragment = fragmentManager
                    .findFragmentById(R.id.frag_list) as DeviceListFragment
                fragment.onInitiateDiscovery()

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ),
                        1001
                    )
                }
                manager!!.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Toast.makeText(
                            this@WifiDirectAction, "Discovery Initiated",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onFailure(reasonCode: Int) {
                        Toast.makeText(
                            this@WifiDirectAction, "Discovery Failed : $reasonCode",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun showDetails(device: WifiP2pDevice?) {
        val fragment: DeviceDetailFragment = fragmentManager
            .findFragmentById(R.id.frag_detail) as DeviceDetailFragment
        fragment.showDetails(device)
    }

    override fun connect(config: WifiP2pConfig?) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                1001
            )
        }
        manager!!.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(
                    this@WifiDirectAction, "Connect failed. Retry.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun disconnect() {
        val fragment: DeviceDetailFragment = fragmentManager
            .findFragmentById(R.id.frag_detail) as DeviceDetailFragment
        fragment.resetViews()
        manager!!.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onFailure(reasonCode: Int) {
                Log.d(TAG, "Disconnect failed. Reason :$reasonCode")
            }

            override fun onSuccess() {
                fragment.getView().setVisibility(View.GONE)
            }
        })
    }

    override fun onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show()
            resetData()
            retryChannel = true
            manager!!.initialize(this, mainLooper, this)
        } else {
            Toast.makeText(
                this,
                "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun cancelDisconnect() {
        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
            val fragment: DeviceListFragment = fragmentManager
                .findFragmentById(R.id.frag_list) as DeviceListFragment
            if (fragment.device == null
                || fragment.device.status === WifiP2pDevice.CONNECTED
            ) {
                disconnect()
            } else if (fragment.device.status === WifiP2pDevice.AVAILABLE
                || fragment.device.status === WifiP2pDevice.INVITED
            ) {
                manager!!.cancelConnect(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Toast.makeText(
                            this@WifiDirectAction, "Aborting connection",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onFailure(reasonCode: Int) {
                        Toast.makeText(
                            this@WiFiDirectActivity,
                            "Connect abort request failed. Reason Code: $reasonCode",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
        }
    }

    companion object {
        const val TAG = "wifidirectdemo"
        private const val PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1001
    }
}