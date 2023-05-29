package com.example.bprac

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.net.Network
import android.net.Uri
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.*
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet.Motion
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.*
import java.net.InetAddress
import java.util.*
import java.util.concurrent.Callable


class MainActivity : AppCompatActivity(), ChannelListener, PeerListListener, ConnectionInfoListener {
    private var channelField: EditText? = null

    private var output: String? = null
    private var pushToggle: Boolean = true

    private var manager: WifiP2pManager? = null
    private var isWifiP2pEnabled = false
    private var retryChannel = false

    private val peers = mutableListOf<WifiP2pDevice>()
    private var currentPeers = mutableListOf<WifiP2pDevice>()
    private var deviceName: String = ""

    private var networkCrewmate: NetworkImposter? = null

    private val intentFilter = IntentFilter().apply {
        addAction(WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
    private var channel: Channel? = null
    private var receiver: BroadcastReceiver? = null

    fun setIsWifiP2pEnabled(isWifiP2pEnabled: Boolean) {
        this.isWifiP2pEnabled = isWifiP2pEnabled
    }

    fun setName(name: String) {
        deviceName = name
    }

    public override fun onResume() {
        super.onResume()
        receiver = WiFiDirectBroadcastReceiver(manager!!, channel!!,this, ::setName)
        registerReceiver(receiver, intentFilter)
    }

    fun EditText.onSubmit(func: () -> Unit) {
        setOnEditorActionListener { _, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_DONE) {
                func()
            }

            true
        }
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    override fun onChannelDisconnected() {
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show()
            retryChannel = true
            manager!!.initialize(this, mainLooper, this)
        } else {
            Toast.makeText(this,
                "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingInflatedId")
    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.CHANGE_NETWORK_STATE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.READ_PHONE_STATE
                ),
                111
            )

        }

        val path = getExternalFilesDir(Environment.getExternalStorageDirectory().absolutePath)?.absolutePath + "/recording.mp3"

        output = path


        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager?.initialize(this, mainLooper, this)

        channelField = findViewById<EditText>(R.id.realChannel)

        channelField!!.onSubmit {
            channelField!!.clearFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(channelField!!.windowToken, 0)
            networkCrewmate?.setGroup(channelField!!.text.toString().toInt())
        }

        var lmanager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!lmanager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "please enable location services", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }

        manager!!.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Toast.makeText(this@MainActivity, "Amongus", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(reasonCode: Int) {
                Toast.makeText(this@MainActivity, "Sussy... " + reasonCode, Toast.LENGTH_SHORT).show()
            }
        })


        val pushToTalk = findViewById<Button>(R.id.push_to_talk)

        pushToTalk.setOnTouchListener { _, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> networkCrewmate?.setRecording(true)
                MotionEvent.ACTION_UP -> networkCrewmate?.setRecording(false)
            }
            true
        }


        val toggle = findViewById<Button>(R.id.toggle)
        toggle.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                ActivityCompat.requestPermissions(this, permissions,0)
            } else {
                networkCrewmate?.setRecording(pushToggle)

                pushToggle = !pushToggle
            }
        }


    }

    companion object {
        private const val TAG = "wpr"
    }

    private fun connectValidPeers()
    {
        val groupOwner = (deviceName == "WPR")

        Toast.makeText(this, "I AM  ${(if (groupOwner) "OWNER" else "JOINER")}", Toast.LENGTH_LONG).show()

        for (device in peers) {
            if (((groupOwner && device.deviceName.contains("WPR") || (!groupOwner && device.deviceName == "WPR"))) && currentPeers.none { peer -> peer.deviceName == device.deviceName }) {
                currentPeers.add(device);

                val config = WifiP2pConfig()
                if (groupOwner)
                    config.groupOwnerIntent = 15
                else
                    config.groupOwnerIntent = 0

                config.deviceAddress = device.deviceAddress
                config.wps.setup = WpsInfo.PBC
                channel?.also { channel ->
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }

                    manager?.connect(channel, config, object : ActionListener {

                        override fun onSuccess() {
                            Log.d("PEER:", "Connected to ${device.deviceName}!")
                        }

                        override fun onFailure(reason: Int) {
                            Log.d("PEER:", "Could not connect... ($reason)")
                            if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                return
                            }
                            manager?.connect(channel, config, object : ActionListener {

                                override fun onSuccess() {
                                    Log.d("PEER:", "Connected to "+device.deviceName+"!")
                                }

                                override fun onFailure(reason: Int) {
                                    Log.d("PEER:", "Could not connect... ($reason)")
                                }
                            })
                        }
                    })
                }
            }
        }
    }
    override fun onPeersAvailable(peerList: WifiP2pDeviceList?) {
        peers.clear()
        peers.addAll(peerList!!.deviceList)
        connectValidPeers()
    }


    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
        if (info!!.groupFormed) {
            Toast.makeText(this, "I AM  ${(if (info!!.isGroupOwner) "SERVER" else "CLIENT")}", Toast.LENGTH_LONG).show()
            if(networkCrewmate == null)
                networkCrewmate = NetworkImposter(this, this@MainActivity, info!!.groupOwnerAddress,8989)

            networkCrewmate!!.initiateConnection(info!!.isGroupOwner)
        }
    }
}
