package com.example.bprac

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class AppStart : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_start)

        val createChannel = findViewById<Button>(R.id.create_channel)
        createChannel.setOnClickListener {
            val intent = Intent(this,ChannelSelectActivity::class.java)
            startActivity(intent)
        }

        val joinChannel = findViewById<Button>(R.id.join_channel)
        joinChannel.setOnClickListener {
            val intent = Intent(this,ChannelSelectActivity::class.java)
            startActivity(intent)
        }
    }
}