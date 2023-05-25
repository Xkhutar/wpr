
//note: this version uses the toggle button to stop audio recording, and the change channel button to play it back.

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
import android.net.Uri
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.*
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.*
import java.net.InetAddress
import java.util.*
import java.util.concurrent.Callable


class MainActivity : AppCompatActivity(), ChannelListener, PeerListListener, ConnectionInfoListener {

    private var output: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var pushToggle: Boolean = false

    @Volatile
    private var ClientPlayer: NetworkAmongus.FileClientAsyncTask? = null

    // END NEW AUDIO STUFF

    private var manager: WifiP2pManager? = null
    private var isWifiP2pEnabled = false
    private var retryChannel = false
    public var networkSus: NetworkSus? = null

    private val peers = mutableListOf<WifiP2pDevice>()
    private var currentPeers = mutableListOf<WifiP2pDevice>()

    public var hostAddress: InetAddress? = null
    private var sendPort: Int = 0
    private var receivePort: Int = 0



    // END NEW CONNECTION
    private var networkCrewmate: NetworkImposter? = null

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: BroadcastReceiver? = null

    fun setIsWifiP2pEnabled(isWifiP2pEnabled: Boolean) {
        this.isWifiP2pEnabled = isWifiP2pEnabled
    }

    public override fun onResume() {
        super.onResume()
        receiver = WiFiDirectBroadcastReceiver(manager!!, channel!!, this::stfu, this)
        registerReceiver(receiver, intentFilter)
    }

    public fun stfu(poop: String){}

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



    @SuppressLint("MissingInflatedId") //cant remember what this is
    @Override //not sure why i needed this either
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // leaving this in to try to get Rory phone to work, currently the recording isn't working for him (setAudioSource issue)
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
        // did some manual permission enabling, which enabled it to boot, but its recording function isn't working

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mediaRecorder = MediaRecorder(this)
        }
        else {
            mediaRecorder = MediaRecorder()//depreciated, using anyways for now
        }

        val path = getExternalFilesDir(Environment.getExternalStorageDirectory().absolutePath)?.absolutePath + "/recording.mp3"

        output = path


        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager?.initialize(this, mainLooper, this)



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

        //discoverService()

        networkSus = NetworkSus(this)


        val pushToTalk = findViewById<Button>(R.id.push_to_talk)
        pushToTalk.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                ActivityCompat.requestPermissions(this, permissions,0)
            } else {
                if (pushToggle)
                    networkCrewmate?.setRecording(false)
                else
                    networkCrewmate?.setRecording(true)

                pushToggle = !pushToggle
            }
        }

        val toggle = findViewById<Button>(R.id.toggle)
        toggle.setOnClickListener {
            //Toast.makeText(this, "Toggling audio input mode (TBC)", Toast.LENGTH_SHORT).show()
            //Temporarily using this as a stop recording button
            //stopRecording()
        }

        val changeChannel = findViewById<Button>(R.id.change_channel)
        changeChannel.setOnClickListener {
            manager!!.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Toast.makeText(this@MainActivity, "Amongus", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(reasonCode: Int) {
                    Toast.makeText(this@MainActivity, "Sussy... " + reasonCode, Toast.LENGTH_SHORT).show()
                }
            })
            Log.d("REGGIE", "HELLO?")
        }

    }

    companion object {
        private const val TAG = "wpr"
    }

    /*
    private fun startRegistration(deviceName: String) {
        Log.d("REGGIE", "MY BODY IS!")

        val record: Map<String, String> = mapOf(
            "magic" to "connect me please!",
            "deviceName" to deviceName
        )
        Log.d("RV", "VAL: "+record["deviceName"])


        val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("_wpr", "_presence._tcp", record)


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager!!.addLocalService(channel, serviceInfo, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("REGGIE", "ADDED LOCAL!!")
            }

            override fun onFailure(arg0: Int) {
                Log.d("REGGIE", "FAILD IDIOT!!")
            }
        })
    }

    private fun discoverService() {
        val txtListener = DnsSdTxtRecordListener { fullDomain, record, device ->
            Log.d("REGGIE", "YAY")
            record["magic"]?.also {
                if (record["magic"] == "connect me please!") {
                    var deviceName = device.deviceName
                    if (deviceName == "") {
                        deviceName = record["deviceName"]
                    }
                    validPeerNames.add(deviceName)
                    Log.d("REGGIE", "Valid peer notified: " + deviceName)
                    connectValidPeers()
                }
            }
        }

        val servListener = DnsSdServiceResponseListener { instanceName, registrationType, resourceType ->
            Log.d("REGGIE", "onBonjourDEEZ $instanceName")
        }

        manager!!.setDnsSdResponseListeners(channel, servListener, txtListener)

        val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()
        manager!!.addServiceRequest(
            channel,
            serviceRequest,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("REGGIE", "DID LISTEN!!");
                }

                override fun onFailure(code: Int) {
                    Log.d("REGGIE", "DID NOT SHUT UP!!!");
                }
            }
        )

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager!!.discoverServices(
            channel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    // Success!
                }

                override fun onFailure(code: Int) {
                    // Command failed. Check for P2P_UNSUPPORTED, ERROR, or BUSY
                    when (code) {
                        WifiP2pManager.P2P_UNSUPPORTED -> {
                            Log.d(TAG, "Wi-Fi Direct isn't supported on this device.")
                        }
                    }
                }
            }
        )

    }
    */


    private fun connectValidPeers()
    {
        for (device in peers) {
            //Log.d("PEER:", "Pogtential->"+device.deviceName);
            if (device.deviceName.contains("WPR") && currentPeers.none { peer -> peer.deviceName == device.deviceName }) {
                currentPeers.add(device);
                Log.d("PEER:", "Found peer!!! -> "+device.deviceName)


                val config = WifiP2pConfig()
                config.deviceAddress = device.deviceAddress
                channel?.also { channel ->
                    Log.d("PEER", "GETTING OWNED?")
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                    Log.d("PEER", "ATTEMPTED CONNECTION TO "+device.deviceName)
                    manager?.connect(channel, config, object : ActionListener {

                        override fun onSuccess() {
                            Log.d("PEER:", "Connected to "+device.deviceName+"!")
                        }

                        override fun onFailure(reason: Int) {
                            Log.d("PEER:", "Could not sus... "+reason)
                            if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                return
                            }
                            manager?.connect(channel, config, object : ActionListener {

                                override fun onSuccess() {
                                    Log.d("PEER:", "Connected to "+device.deviceName+"!")
                                }

                                override fun onFailure(reason: Int) {
                                    Log.d("PEER:", "Could not sus... "+reason)
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

    fun setAddress(hostAddress: InetAddress) {
        Log.d(TAG, "Got client address! ->"+hostAddress.toString())
        this.hostAddress = hostAddress
    }
/*
    fun startServer() {
        Log.d("BIGBOY", "HUGE STARTING!!!")
        val serverThread = Thread(NetworkSus.FileServerAsyncTask(receivePort))
        serverThread.priority = 10
        serverThread.start()
        Thread.sleep(1000)
        ClientPlayer = NetworkAmongus.FileClientAsyncTask(this@MainActivity, hostAddress!!, sendPort)
        val clientThread = Thread(ClientPlayer)
        clientThread.priority = 9
        clientThread.start()

    }
    */
    fun startServer(isServer: Boolean) {
        Log.d("BIGBOY", "HUGE STARTING!!!")
        networkCrewmate = NetworkImposter(this, this@MainActivity, hostAddress!!, 8989)
        Thread({
            networkCrewmate!!.initiateConnection(isServer)
        }).start()
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
        Log.d("CONNECT", "GOT INFO")
        Log.d("CONNECT", "GFORM? "+info!!.groupFormed+" ZIMBA? "+info!!.groupOwnerAddress)


        if (info!!.groupFormed && info!!.isGroupOwner) {
            Log.d(TAG, "I AM SERVER!")
            sendPort = 8989
            receivePort = 8989
            NetworkImposter.ServerHandshake(receivePort, ::setAddress, ::startServer).execute()
        } else if (info!!.groupFormed) {
            Log.d(TAG, "I AM CLIENT!")
            hostAddress = info!!.groupOwnerAddress
            sendPort = 8989
            receivePort = 8989
            NetworkImposter.ClientHandshake(sendPort, hostAddress!!, ::startServer).execute()
        }
    }
}
