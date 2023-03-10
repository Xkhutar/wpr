
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
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.*
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
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*


class MainActivity : AppCompatActivity(), ChannelListener, PeerListListener, ConnectionInfoListener {

    private var output: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var state: Boolean = false
    private var pushToggle: Boolean = false
    var mMediaPlayer: MediaPlayer? = null //private?

    private var manager: WifiP2pManager? = null
    private var isWifiP2pEnabled = false
    private var retryChannel = false
    public var networkSus: NetworkSus? = null

    private val peers = mutableListOf<WifiP2pDevice>()
    private var currentPeer: WifiP2pDevice? = null

    public var hostAddress: InetAddress? = null
    private var sendPort: Int = 0
    private var receivePort: Int = 0

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
        receiver = WiFiDirectBroadcastReceiver(manager!!, channel!!, this)
        registerReceiver(receiver, intentFilter)
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    public fun connect(config: WifiP2pConfig) {
        try {
            manager!!.connect(channel, config, object : ActionListener {

                override fun onSuccess() {
                    Log.d(TAG, "COnnect yay!");
                }

                override fun onFailure(reason: Int) {
                    Toast.makeText(this@MainActivity, "Connect failed. Retry.", Toast.LENGTH_SHORT)
                        .show();
                }
            })
        }
        catch (e: SecurityException)
        {
            Log.d(TAG, "Amongus yay!");
        }
    }

    public fun disconnect() {

        manager!!.removeGroup(channel, object : ActionListener {

            override fun onFailure(reasonCode: Int) {
                Log.d(TAG, "Disconnect failed. Reason :$reasonCode")

            }

            override fun onSuccess() {
                Log.d(TAG, "Disconnect yay!")
            }

        })
    }

    override fun onChannelDisconnected() {
        // we will try once more
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
/*
    override fun cancelDisconnect() {

        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
            val fragment = supportFragmentManager
                .findFragmentById(R.id.frag_list) as DeviceListFragment
            if (fragment.device == null || fragment.device!!.status == WifiP2pDevice.CONNECTED) {
                disconnect()
            } else if (fragment.device!!.status == WifiP2pDevice.AVAILABLE || fragment.device!!.status == WifiP2pDevice.INVITED) {

                manager!!.cancelConnect(channel, object : ActionListener {

                    override fun onSuccess() {
                        Toast.makeText(this@MainActivity, "Aborting connection",
                            Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(reasonCode: Int) {
                        Toast.makeText(this@MainActivity,
                            "Connect abort request failed. Reason Code: $reasonCode",
                            Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }
*/

    private fun createGroup() {
        try {
            manager?.also { manager ->

                manager.requestGroupInfo(channel) { group ->
                    Log.d(TAG, "createGroup group:$group")
                }

                manager.createGroup(channel, object : ActionListener {
                    override fun onSuccess() {
                        Toast.makeText(this@MainActivity, "create group success", Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(reason: Int) {
                        Toast.makeText(this@MainActivity, "create group failure", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
        catch(e: SecurityException)
        {
            Log.d(TAG, "You are stupid and dumb");
        }
    }

    fun playRecording(uri: Uri) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mediaRecorder = MediaRecorder(this)
        }
        else {
            mediaRecorder = MediaRecorder()//depreciated, using anyways for now
        }

        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) //correct format?
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)    //correct encoder?
        mediaRecorder?.setOutputFile(output)

        var mMediaPlayer: MediaPlayer? = null
        try {
            mMediaPlayer = MediaPlayer().apply {
                setDataSource(application, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                prepare()
                start()
            }
        } catch (exception: IOException) {
            mMediaPlayer?.release()
            mMediaPlayer = null
        }


    }


    private fun startRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mediaRecorder = MediaRecorder(this)
        }
        else {
            mediaRecorder = MediaRecorder()//depreciated, using anyways for now
        }

        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) //correct format?
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)    //correct encoder?
        mediaRecorder?.setOutputFile(output)

        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            state = true
            Toast.makeText(this, "Recording started!", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopRecording(){
        if(state){
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
            state = false
            Toast.makeText(this, "stopping recording", Toast.LENGTH_SHORT).show()

            attemptSendAudio()
        }else{
            Toast.makeText(this, "You are not recording right now!", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "please enable location servicedededes", Toast.LENGTH_LONG).show()
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
                    stopRecording()
                else
                    startRecording()

                pushToggle = !pushToggle
            }
        }

        val toggle = findViewById<Button>(R.id.toggle)
        toggle.setOnClickListener {
            //Toast.makeText(this, "Toggling audio input mode (TBC)", Toast.LENGTH_SHORT).show()
            //Temporarily using this as a stop recording button
            //stopRecording()
            attemptSendAudio()
        }

        val changeChannel = findViewById<Button>(R.id.change_channel)
        changeChannel.setOnClickListener {
            //val intent = Intent(this,AppStart::class.java)
            // startActivity(intent)
            //these two lines are the correct function, but temporarily using this as the playback button

            //val file = File(Environment.getExternalStorageDirectory(), "recording.mp3")
            val file = File(path) //File(getFilesDir().toString() + "/recording.mp3")
            val uri = Uri.fromFile(file)
            playRecording(uri)
        }

    }

    companion object {
        private const val TAG = "wpr"
    }
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
    fun attemptSendAudio() {
        NetworkAmongus.FileClientAsyncTask(this@MainActivity, output!!, hostAddress!!, 8988).execute()
    }

    fun playAudio() {
        playRecording(Uri.fromFile(File(output)))
    }

    override fun onPeersAvailable(peerList: WifiP2pDeviceList?) {
        val refreshedPeers = peerList!!.deviceList
        if (refreshedPeers != peers) {
            peers.clear()
            peers.addAll(refreshedPeers)

            for (device in peers)
            {
                if(device.deviceName.contains("WPR") && (currentPeer == null || currentPeer!!.deviceName != device.deviceName))
                {
                    currentPeer = device
                    Log.d("PEER:", "Found my peer!!! -> "+currentPeer!!.deviceName)

                    val config = WifiP2pConfig()
                    config.deviceAddress = device.deviceAddress
                    channel?.also { channel ->
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return
                        }
                        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {

                            override fun onSuccess() {
                                Log.d("PEER:", "Connected to "+device.deviceName+"!")
                            }

                            override fun onFailure(reason: Int) {
                                Log.d("PEER:", "Could not sus...")
                            }
                        }
                        )}

                }
            }
        }

        if (peers.isEmpty()) {
            Log.d(TAG, "No devices found")
        }
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
        Log.d("CONNECT", "EITHER TBH")
        if (info!!.groupFormed && info!!.isGroupOwner) {
            NetworkSus.FileServerAsyncTask(this@MainActivity, output!!, 8988, ::playAudio).execute()
        } else if (info!!.groupFormed) {
            println("I AM CLIENT!")
            hostAddress = info!!.groupOwnerAddress
        }
    }

}
