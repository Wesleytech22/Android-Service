package com.android.musicsplayerapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var startBtn: Button
    private lateinit var pauseBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var statusText: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var timeText: TextView

    private var musicService: MusicService? = null
    private var isBound = false
    private var updateJob: Job? = null
    private var isUserSeeking = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.LocalBinder
            musicService = binder.getService()
            isBound = true
            updateUI()
            startUpdatingSeekBar()
            Toast.makeText(this@MainActivity, "✅ Serviço conectado", Toast.LENGTH_SHORT).show()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            musicService = null
            updateJob?.cancel()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupListeners()

        // Conectar ao serviço
        Intent(this, MusicService::class.java).also { intent ->
            startService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun initializeViews() {
        startBtn = findViewById(R.id.start_btn)
        pauseBtn = findViewById(R.id.pause_btn)
        stopBtn = findViewById(R.id.stop_btn)
        statusText = findViewById(R.id.text_view)
        seekBar = findViewById(R.id.seek_bar)
        timeText = findViewById(R.id.time_text)

        // Estado inicial
        pauseBtn.isEnabled = false
        stopBtn.isEnabled = true
        seekBar.max = 100
    }

    private fun setupListeners() {
        startBtn.setOnClickListener {
            musicService?.playMusic()
            updateUI()
            Toast.makeText(this, "▶️ Reproduzindo", Toast.LENGTH_SHORT).show()
        }

        pauseBtn.setOnClickListener {
            musicService?.pauseMusic()
            updateUI()
            Toast.makeText(this, "⏸️ Pausado", Toast.LENGTH_SHORT).show()
        }

        stopBtn.setOnClickListener {
            musicService?.stopMusic()
            updateUI()
            seekBar.progress = 0 // ⬅️ Reset visual do seekbar
            Toast.makeText(this, "⏹️ Parado", Toast.LENGTH_SHORT).show()
        }

        // Configurar SeekBar para arrastar
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Se o usuário está arrastando, atualizar tempo visual
                    val duration = musicService?.getDuration() ?: 30000
                    val newPosition = (progress * duration) / 100
                    val time = formatTime(newPosition)
                    val totalTime = formatTime(duration)
                    timeText.text = "$time / $totalTime"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
                updateJob?.cancel() // Pausar atualização automática
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                // Definir nova posição no serviço
                val progress = seekBar?.progress ?: 0
                val duration = musicService?.getDuration() ?: 30000
                val newPosition = (progress * duration) / 100
                musicService?.seekTo(newPosition)

                // Retomar atualização
                startUpdatingSeekBar()
            }
        })
    }

    private fun updateUI() {
        val isPlaying = musicService?.isMusicPlaying() ?: false

        statusText.text = if (isPlaying) {
            "🎵 Tocando"
        } else if (musicService != null) {
            "⏸️ Pausado"
        } else {
            "⏹️ Serviço não conectado"
        }

        startBtn.isEnabled = !isPlaying
        pauseBtn.isEnabled = isPlaying
        stopBtn.isEnabled = (musicService != null)
    }

    private fun startUpdatingSeekBar() {
        updateJob?.cancel()
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isBound && isActive) {
                try {
                    if (!isUserSeeking) { // Só atualizar se o usuário não estiver arrastando
                        musicService?.let { service ->
                            val currentPos = service.getCurrentPosition()
                            val duration = service.getDuration()

                            if (duration > 0) {
                                val progress = (currentPos * 100) / duration
                                seekBar.progress = progress

                                val currentTime = formatTime(currentPos)
                                val totalTime = formatTime(duration)
                                timeText.text = "$currentTime / $totalTime"
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro: ${e.message}")
                }
                delay(1000) // Atualizar a cada segundo
            }
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    override fun onStop() {
        super.onStop()
        updateJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}