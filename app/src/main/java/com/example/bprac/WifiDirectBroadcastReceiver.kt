package com.example.bprac

import android.content.BroadcastReceiver
import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import android.content.Intent
import com.example.bprac.R
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.net.NetworkInfo
import android.os.Parcelable
import android.net.wifi.p2p.WifiP2pDevice
import android.util.Log

class WifiDirectBroadcastReceiver : BroadcastReceiver() {
    private var manager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var activity: WifiDirectAction? = null

    /**
     * @param manager WifiP2pManager system service
     * @param channel Wifi p2p channel
     * @param activity activity associated with the receiver
     */
    fun WiFiDirectBroadcastReceiver(
        manager: WifiP2pManager?, channel: WifiP2pManager.Channel?,
        activity: WifiDirectAction?) {
        //super()
        this.manager = manager
        this.channel = channel
        this.activity = activity
    }

    /*
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {
            // UI update to indicate wifi p2p status.
            val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                activity?.setIsWifiP2pEnabled(true)
            } else {
                activity?.setIsWifiP2pEnabled(false)
                activity?.resetData()
            }
            Log.d(WifiDirectAction.TAG, "P2P state changed - $state")
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {
            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (manager != null) {
                manager!!.requestPeers(
                    channel, activity.getFragmentManager()
                        .findFragmentById(R.id.frag_list) as PeerListListener
                )
            }
            Log.d(WifiDirectAction.TAG, "P2P peers changed")
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {
            if (manager == null) {
                return
            }
            val networkInfo = intent
                .getParcelableExtra<Parcelable>(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo?
            if (networkInfo!!.isConnected) {
                // we are connected with the other device, request connection
                // info to find group owner IP
                val fragment: DeviceDetailFragment = activity
                    ?.getFragmentManager().findFragmentById(R.id.frag_detail) as DeviceDetailFragment
                manager!!.requestConnectionInfo(channel, fragment)
            } else {
                // It's a disconnect
                activity?.resetData()
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action) {
            val fragment: DeviceListFragment = activity?.getFragmentManager()?.findFragmentById(R.id.frag_list) as DeviceListFragment
            fragment.updateThisDevice(
                intent.getParcelableExtra<Parcelable>(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE
                ) as WifiP2pDevice
            )
        }
    }
}