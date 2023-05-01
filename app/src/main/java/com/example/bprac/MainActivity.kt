
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
    private var state: Boolean = false
    private var pushToggle: Boolean = false

    // NEW AUDIO STUFF
    private var bigBuffer: ByteArray = ByteArray(10000)
    private var bufferIndex: Int = 0

    @Volatile
    private var currentlyRecording: Boolean = false
    private var audioRecorder: AudioRecord? = null
    private var recorderThread: Thread? = null

    private var currentlyPlaying: Boolean  = false
    private var audioPlayer: AudioTrack? = null
    private var playerThread: Thread? = null

    private var ClientPlayer: NetworkAmongus.FileClientAsyncTask? = null

    // END NEW AUDIO STUFF

    private var manager: WifiP2pManager? = null
    private var isWifiP2pEnabled = false
    private var retryChannel = false
    public var networkSus: NetworkSus? = null

    private val peers = mutableListOf<WifiP2pDevice>()
    private var currentPeer: WifiP2pDevice? = null

    public var hostAddress: InetAddress? = null
    private var sendPort: Int = 0
    private var receivePort: Int = 0

    private var server: Callable<Any>? = null

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

    fun playRecording() {
        audioPlayer = AudioTrack(
            AudioManager.STREAM_SYSTEM,
            8192,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            10000,
            AudioTrack.MODE_STATIC
        )
        audioPlayer!!.write(bigBuffer, 0, 10000)
        audioPlayer!!.play()
    }


    private fun startRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        audioRecorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            8192,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            4096
        )

        audioRecorder!!.startRecording()
        currentlyRecording = true
        recorderThread = Thread({ readAudioData() }, "AudioRecorder Thread")
        recorderThread!!.start()
    }

    private fun readAudioData() {
        while (currentlyRecording) {
            try {
                val amountToRead = 4096

                if (amountToRead + bufferIndex > 10000) {
                    audioRecorder!!.read(bigBuffer, bufferIndex, 10000 - bufferIndex)
                    bufferIndex = amountToRead - (10000 - bufferIndex)
                    audioRecorder!!.read(bigBuffer, 0, bufferIndex)
                } else {
                    audioRecorder!!.read(bigBuffer, bufferIndex, amountToRead)
                    bufferIndex += amountToRead
                }


                Log.d("AUDIO", "READ TO "+ bufferIndex + "!")
            }
            catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        if (audioRecorder != null) {
            currentlyRecording = false
            audioRecorder!!.stop()
            audioRecorder!!.release()
            audioRecorder = null

            recorderThread = null
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
                    ClientPlayer?.setRecording(false)
                else
                    ClientPlayer?.setRecording(true)

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
            //val intent = Intent(this,AppStart::class.java)
            // startActivity(intent)
            //these two lines are the correct function, but temporarily using this as the playback button

            //val file = File(Environment.getExternalStorageDirectory(), "recording.mp3")
            val file = File(path) //File(getFilesDir().toString() + "/recording.mp3")
            val uri = Uri.fromFile(file)
            playRecording()
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

    fun setAddress(hostAddress: InetAddress) {
        Log.d(TAG, "Got client address! ->"+hostAddress.toString())
        this.hostAddress = hostAddress
    }

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

    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
        Log.d("CONNECT", "GOT INFO")
        if (info!!.groupFormed && info!!.isGroupOwner) {
            Log.d(TAG, "I AM SERVER!")
            sendPort = 8998
            receivePort = 8989
            NetworkSus.ServerHandshake(receivePort, ::setAddress, ::startServer).execute()
        } else if (info!!.groupFormed) {
            Log.d(TAG, "I AM CLIENT!")
            hostAddress = info!!.groupOwnerAddress
            sendPort = 8989
            receivePort = 8998
            NetworkAmongus.ClientHandshake(sendPort, hostAddress!!, ::startServer).execute()
        }
    }
}
