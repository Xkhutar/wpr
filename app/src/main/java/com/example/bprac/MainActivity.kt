//note: this version uses the toggle button to stop audio recording, and the change channel button to play it back.
// app will crash when record is pushed again. Went back to older (just recording) version and bug existed there too, odd I didn't catch it before.
// eventually the bytes will be sent out and not saved locally so perhaps this bug doesn't need to be fixed
// if you comment out the release() of mediarecorder in the stopRecording, it will eliminate the crash, but the recording functionality will still be broken post-stop

package com.example.bprac

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
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

        mediaRecorder = MediaRecorder() //depreciated, using anyways for now
        output = Environment.getExternalStorageDirectory().absolutePath + "/recording.mp3"  //path to the root of our external storage and add our recording name and filetype to it


        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) //correct format?
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)    //correct encoder?
        mediaRecorder?.setOutputFile(output)

        fun playRecording(uri: Uri) {
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

        fun stopRecording(){ //tutorial has these as private, but it doesn't like that, so removing that : "'private' not applicable to local function"
            if(state){
                mediaRecorder?.stop()
                mediaRecorder?.release()
                state = false
            }else{
                Toast.makeText(this, "You are not recording right now!", Toast.LENGTH_SHORT).show()
            }
        }


        val pushToTalk = findViewById<Button>(R.id.push_to_talk)
        pushToTalk.setOnClickListener {
            //Toast.makeText(this, "Transmitting (TBC)", Toast.LENGTH_SHORT).show()
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
            stopRecording()
        }

        val changeChannel = findViewById<Button>(R.id.change_channel)
        changeChannel.setOnClickListener {
            //val intent = Intent(this,AppStart::class.java)   //these two lines are the correct function, but temporarily using this as the playback button
            // startActivity(intent)
            val file = File(Environment.getExternalStorageDirectory(), "recording.mp3")
            val uri = Uri.fromFile(file)
            playRecording(uri)
        }

    }
}