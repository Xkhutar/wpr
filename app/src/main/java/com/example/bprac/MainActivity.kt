//note: this version uses the toggle button to stop audio recording, and the change channel button to play it back.

package com.example.bprac

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
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

class MainActivity : AppCompatActivity() {

    private var output: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var state: Boolean = false
    private var recordingStopped: Boolean = false
    var mMediaPlayer: MediaPlayer? = null //private?

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

}