package com.android.musicsplayerapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SongSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var doneButton: Button
    private lateinit var addAllButton: Button
    private lateinit var queueSizeText: TextView
    private var musicService: MusicService? = null
    private var isBound = false

    private val availableSongs = Song.getSampleSongs()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.LocalBinder
            musicService = binder.getService()
            isBound = true
            setupRecyclerView()
            updateQueueSize()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            musicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        recyclerView = findViewById(R.id.songs_recycler_view)
        doneButton = findViewById(R.id.done_button)
        addAllButton = findViewById(R.id.add_all_button)
        queueSizeText = findViewById(R.id.queue_size_text)

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        // Botão para adicionar todas as músicas
        addAllButton.setOnClickListener {
            // Adicionar uma por uma (método seguro)
            availableSongs.forEach { song ->
                musicService?.addToQueue(song)
            }
            updateQueueSize()
            Toast.makeText(this, "Todas as músicas adicionadas à fila", Toast.LENGTH_SHORT).show()
        }

        // Conectar ao serviço
        Intent(this, MusicService::class.java).also { intent ->
            bindService(intent, connection, BIND_AUTO_CREATE)
        }

        doneButton.setOnClickListener {
            val queueSize = musicService?.getQueueSize() ?: 0
            if (queueSize > 0) {
                finish()
            } else {
                Toast.makeText(this, "Adicione músicas à fila primeiro", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        val adapter = SongAdapter(availableSongs,
            onItemClick = { song ->
                // Tocar música imediatamente (substitui a fila atual)
                musicService?.clearQueue()
                musicService?.addToQueue(song)
                musicService?.playMusic()
                Toast.makeText(this, "Tocando: ${song.title}", Toast.LENGTH_SHORT).show()
                updateQueueSize()
                finish()
            },
            onAddToQueueClick = { song ->
                // Adicionar ao final da fila
                musicService?.addToQueue(song)
                updateQueueSize()
                Toast.makeText(this, "✓ ${song.title} adicionada", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerView.adapter = adapter
    }

    private fun updateQueueSize() {
        val queueSize = musicService?.getQueueSize() ?: 0
        queueSizeText.text = "Fila: $queueSize música${if (queueSize != 1) "s" else ""}"

        if (queueSize > 0) {
            doneButton.text = "✅ VOLTAR AO PLAYER ($queueSize)"
        } else {
            doneButton.text = "✅ VOLTAR AO PLAYER"
        }
    }

    override fun onResume() {
        super.onResume()
        updateQueueSize()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
        }
    }
}