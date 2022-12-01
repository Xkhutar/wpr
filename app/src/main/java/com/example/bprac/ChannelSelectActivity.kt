package com.example.bprac

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class ChannelSelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.channel_select)

        val chanId:EditText=findViewById(R.id.input_channel)

        val joinChannel = findViewById<Button>(R.id.join_channel2)
        joinChannel.setOnClickListener {
            val txtBoxInput = chanId.text.toString()
            if(txtBoxInput.isEmpty()){
                Toast.makeText(this, "Enter a valid channel ID", Toast.LENGTH_SHORT).show()
            }
            else{
                val intent = Intent(this,MainActivity::class.java)
                startActivity(intent)
            }
        }
    }
}