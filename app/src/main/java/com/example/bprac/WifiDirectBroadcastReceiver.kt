/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bprac

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Network
import com.example.bprac.R
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.*
import android.os.Parcelable
import android.util.Log
import androidx.core.app.ActivityCompat


class WiFiDirectBroadcastReceiver(manager: WifiP2pManager, channel: Channel, activity: MainActivity, receptacle: (String) -> Unit) : BroadcastReceiver() {
    private var manager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var activity: MainActivity? = null
    public  var device: WifiP2pDevice? = null
    private var receptacle: (String) -> Unit

    /**
     * @param manager WifiP2pManager system service
     * @param channel Wifi p2p channel
     * @param activity activity associated with the receiver
     */
    init {
        //super()
        this.manager = manager
        this.channel = channel
        this.activity = activity
        this.receptacle = receptacle
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "GOT STATE " + action)

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {
            val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.d(TAG, "WIFI-D ENABLED!!!")
                activity!!.setIsWifiP2pEnabled(true)
            } else {
                Log.d(TAG, "WIFI-D DISABLED...")
                activity!!.setIsWifiP2pEnabled(false)

            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {
            if (manager != null) {
                if (ActivityCompat.checkSelfPermission(activity!!.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
                manager!!.requestPeers(channel, activity)
            }
            Log.d(TAG, "PEERS ARE AMONG US")

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action) {
            device = intent.getParcelableExtra(EXTRA_WIFI_P2P_DEVICE)!!
            Log.d("NAME", "MY NAME IS " + device!!.deviceName)
            receptacle(device!!.deviceName)
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {
            manager!!.requestConnectionInfo(channel!!, activity!!)
        }
    }

    companion object {
        private const val TAG = "SUS"
    }
}
