package com.android.musicsplayerapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startBtn = findViewById<Button>(R.id.start_btn)
        val stopBtn = findViewById<Button>(R.id.stop_btn)
        val statusText = findViewById<TextView>(R.id.text_view)

        startBtn.setOnClickListener {
            showToast("🎵 Serviço de música iniciado!", Toast.LENGTH_SHORT)
            startService()
            statusText.text = "▶️ Música Rodando"
        }

        stopBtn.setOnClickListener {
            showToast("⏹️ Serviço de música parado!", Toast.LENGTH_SHORT)
            stopService()
            statusText.text = "The Services App"
        }
    }

    private fun startService() {
        val serviceIntent = Intent(this, MusicService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopService() {
        val serviceIntent = Intent(this, MusicService::class.java)
        stopService(serviceIntent)
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

    //Necessario ajuste na comunicação do serviço
}