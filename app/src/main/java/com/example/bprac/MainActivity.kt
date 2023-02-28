//note: this version uses the toggle button to stop audio recording, and the change channel button to play it back.

package com.example.bprac

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager.*
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View

import java.util.*

class MainActivity : AppCompatActivity(), ChannelListener {

    private var output: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var state: Boolean = false
    private var recordingStopped: Boolean = false
    var mMediaPlayer: MediaPlayer? = null //private?

    private var manager: WifiP2pManager? = null
    private var isWifiP2pEnabled = false
    private var retryChannel = false

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
        receiver?.also { receiver ->
            registerReceiver(receiver, intentFilter)
        }
    }

    public override fun onPause() {
        super.onPause()
        receiver?.also { receiver ->
            unregisterReceiver(receiver)
        }
    }

    override fun connect(config: WifiP2pConfig) {
        manager!!.connect(channel, config, object : ActionListener {

            override fun onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(this@WiFiDirectActivity, "Connect failed. Retry.",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun disconnect() {
        val fragment = supportFragmentManager
            .findFragmentById(R.id.frag_detail) as DeviceDetailFragment
        fragment.resetViews()
        manager!!.removeGroup(channel, object : ActionListener {

            override fun onFailure(reasonCode: Int) {
                Log.d(TAG, "Disconnect failed. Reason :$reasonCode")

            }

            override fun onSuccess() {
                fragment.view!!.visibility = View.GONE
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
            Toast.makeText(this,
                "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                Toast.LENGTH_LONG).show()
        }
    }

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
                        Toast.makeText(this@WiFiDirectActivity, "Aborting connection",
                            Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(reasonCode: Int) {
                        Toast.makeText(this@WiFiDirectActivity,
                            "Connect abort request failed. Reason Code: $reasonCode",
                            Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }


    private fun createGroup() {
        manager?.also { manager ->

            manager.requestGroupInfo(channel) { group ->
                Log.d(TAG, "createGroup group:$group")
            }

            manager.createGroup(channel, object : ActionListener {
                override fun onSuccess() {
                    Toast.makeText(this@WiFiDirectActivity, "create group success", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(reason: Int) {
                    Toast.makeText(this@WiFiDirectActivity, "create group failure", Toast.LENGTH_SHORT).show()
                }
            })
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

        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) //correct format?
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)    //correct encoder?
        mediaRecorder?.setOutputFile(output)


        fun playRecording(uri: Uri) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mediaRecorder = MediaRecorder(this)
            }
            else {
                mediaRecorder = MediaRecorder()//depreciated, using anyways for now
            }

        output = path

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

            manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
            channel = manager?.initialize(this, mainLooper, null)
            channel?.also { channel ->
                receiver = WiFiDirectBroadcastReceiver(manager, channel, this)
            }
        }


        fun startRecording() {
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

        fun stopRecording(){
            if(state){
                mediaRecorder?.stop()
                mediaRecorder?.reset()
                mediaRecorder?.release()
                mediaRecorder = null
                state = false
                Toast.makeText(this, "stopping recording", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this, "You are not recording right now!", Toast.LENGTH_SHORT).show()
            }
        }


        val pushToTalk = findViewById<Button>(R.id.push_to_talk)
        pushToTalk.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                ActivityCompat.requestPermissions(this, permissions,0)
            } else {
                startRecording()
            }

        }

        val toggle = findViewById<Button>(R.id.toggle)
        toggle.setOnClickListener {
            //Toast.makeText(this, "Toggling audio input mode (TBC)", Toast.LENGTH_SHORT).show()
            //Temporarily using this as a stop recording button
            stopRecording()
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

}