package com.example.bprac

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var playback: MediaPlayer
    val RECORDER_SAMPLE_RATE = 44100

    @RequiresApi(Build.VERSION_CODES.S)

    @SuppressLint("MissingInflatedId")
    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val path:String = Environment.getExternalStorageDirectory().absolutePath + "/recording.3gp"

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED) {
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
        var isRecording = false
        var isPlaying = false

        val pushToTalk = findViewById<Button>(R.id.push_to_talk)
        pushToTalk.setOnClickListener {
            Toast.makeText(this, "Transmitting (TBC)", Toast.LENGTH_SHORT).show()
            
            //media recording code:
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()
            //mediaRecorder = MediaRecorder(this.applicationContext)

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setAudioSamplingRate(RECORDER_SAMPLE_RATE)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mediaRecorder.setOutputFile(path)

            if(!isRecording) {
                mediaRecorder.prepare()
                mediaRecorder.start()
                isRecording = true
            }
            else{
                isRecording = false
                mediaRecorder.stop()
                mediaRecorder.release()
            }

        }
        
        val toggle = findViewById<Button>(R.id.toggle)
        toggle.setOnClickListener {
            Toast.makeText(this, "Toggling audio input mode (TBC)", Toast.LENGTH_SHORT).show()

            playback = MediaPlayer()
            playback.setDataSource(path)
            if(!isPlaying) {
                playback.prepare()
                playback.start()
                isPlaying = true
            }
            else{
                isPlaying = false
                playback.stop()
                playback.release()
            }

        }

        val changeChannel = findViewById<Button>(R.id.change_channel)
        changeChannel.setOnClickListener {
            val intent = Intent(this,AppStart::class.java)
            startActivity(intent)
        }
    }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray )
        {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if(requestCode==111 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //binding.button1.isEnabled = true
                Toast.makeText(this, "permissions granted", Toast.LENGTH_SHORT).show()
        }
    }

    
}