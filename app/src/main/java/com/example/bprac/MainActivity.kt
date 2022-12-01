package com.example.bprac

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pushToTalk = findViewById<Button>(R.id.join_channel)
        pushToTalk.setOnClickListener {
            Toast.makeText(this, "Transmitting (TBC)", Toast.LENGTH_SHORT).show()
        }

        val toggle = findViewById<Button>(R.id.toggle)
        toggle.setOnClickListener {
            Toast.makeText(this, "Toggling audio input mode (TBC)", Toast.LENGTH_SHORT).show()
        }

        val changeChannel = findViewById<Button>(R.id.change_channel)
        changeChannel.setOnClickListener {
            val intent = Intent(this,AppStart::class.java)
            startActivity(intent)
        }


    }
}