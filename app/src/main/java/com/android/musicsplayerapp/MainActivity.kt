package com.android.musicsplayerapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var startBtn: Button
    private lateinit var pauseBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var statusText: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var timeText: TextView
    private lateinit var currentSongTitle: TextView
    private lateinit var currentSongArtist: TextView
    private lateinit var queueRecyclerView: RecyclerView
    private lateinit var addSongsButton: Button
    private lateinit var clearQueueButton: Button
    private lateinit var repeatButton: Button
    private lateinit var nextButton: Button
    private lateinit var prevButton: Button
    private lateinit var shuffleButton: Button  // Remover se não existir no layout
    private lateinit var queueSizeText: TextView  // Remover se não existir no layout

    private var musicService: MusicService? = null
    private var isBound = false
    private var updateJob: Job? = null
    private var isUserSeeking = false
    private lateinit var queueAdapter: QueueAdapter

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.LocalBinder
            musicService = binder.getService()
            isBound = true
            updateUI()
            updateQueueUI()
            updateCurrentSongUI()
            updateRepeatButton(musicService?.getRepeatMode() ?: MusicService.RepeatMode.NONE)
            // updateShuffleButton(musicService?.isShuffleEnabled() ?: false)  // Comentar se não tiver shuffle
            startUpdatingSeekBar()
            Log.d("MainActivity", "✅ Serviço conectado")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            musicService = null
            updateJob?.cancel()
            Log.d("MainActivity", "❌ Serviço desconectado")
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

        // Verificar se veio da notificação
        handleIntent(intent)
    }

    // REMOVA ou COMENTE este método se não for necessário
    // override fun onNewIntent(intent: Intent?) {
    //     super.onNewIntent(intent)
    //     handleIntent(intent)
    // }

    private fun handleIntent(intent: Intent?) {
        // Não precisa fazer nada aqui se não for usar intents especiais
    }

    private fun initializeViews() {
        startBtn = findViewById(R.id.start_btn)
        pauseBtn = findViewById(R.id.pause_btn)
        stopBtn = findViewById(R.id.stop_btn)
        statusText = findViewById(R.id.text_view)
        seekBar = findViewById(R.id.seek_bar)
        timeText = findViewById(R.id.time_text)
        currentSongTitle = findViewById(R.id.current_song_title)
        currentSongArtist = findViewById(R.id.current_song_artist)
        queueRecyclerView = findViewById(R.id.queue_recycler_view)
        addSongsButton = findViewById(R.id.add_songs_button)
        clearQueueButton = findViewById(R.id.clear_queue_button)
        repeatButton = findViewById(R.id.repeat_button)
        nextButton = findViewById(R.id.next_button)
        prevButton = findViewById(R.id.prev_button)

        // COMENTE estas linhas se os IDs não existirem no layout
        // shuffleButton = findViewById(R.id.shuffle_button)
        // queueSizeText = findViewById(R.id.queue_size_text)

        // Estado inicial
        pauseBtn.isEnabled = false
        stopBtn.isEnabled = true
        seekBar.max = 100

        // Configurar RecyclerView
        queueRecyclerView.layoutManager = LinearLayoutManager(this)
        queueRecyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        queueAdapter = QueueAdapter(
            emptyList(),
            onRemoveClick = { index ->
                val removed = musicService?.removeFromQueue(index) ?: false
                if (removed) {
                    updateQueueUI()
                    updateCurrentSongUI()
                    Toast.makeText(this, "Removido da fila", Toast.LENGTH_SHORT).show()
                }
            }
        )
        queueRecyclerView.adapter = queueAdapter
    }

    private fun setupListeners() {
        startBtn.setOnClickListener {
            musicService?.playMusic()
            updateUI()
            updateCurrentSongUI()
        }

        pauseBtn.setOnClickListener {
            musicService?.pauseMusic()
            updateUI()
        }

        stopBtn.setOnClickListener {
            musicService?.stopMusic()
            updateUI()
            updateCurrentSongUI()
            seekBar.progress = 0
            timeText.text = "00:00 / ${formatTime(musicService?.getDuration() ?: 30000)}"
        }

        addSongsButton.setOnClickListener {
            val intent = Intent(this, SongSelectionActivity::class.java)
            startActivity(intent)
        }

        clearQueueButton.setOnClickListener {
            musicService?.clearQueue()
            updateQueueUI()
            updateCurrentSongUI()
            Toast.makeText(this, "Fila limpa", Toast.LENGTH_SHORT).show()
        }

        repeatButton.setOnClickListener {
            musicService?.let { service ->
                val currentMode = service.getRepeatMode()
                val nextMode = when (currentMode) {
                    MusicService.RepeatMode.NONE -> MusicService.RepeatMode.ONE
                    MusicService.RepeatMode.ONE -> MusicService.RepeatMode.ALL
                    MusicService.RepeatMode.ALL -> MusicService.RepeatMode.NONE
                }
                service.setRepeatMode(nextMode)
                updateRepeatButton(nextMode)
            }
        }

        // COMENTE este bloco se não tiver botão shuffle
        // shuffleButton.setOnClickListener {
        //     musicService?.toggleShuffle()
        //     updateShuffleButton(musicService?.isShuffleEnabled() ?: false)
        //     Toast.makeText(this,
        //         if (musicService?.isShuffleEnabled() == true) "Modo embaralhado" else "Ordem normal",
        //         Toast.LENGTH_SHORT).show()
        // }

        nextButton.setOnClickListener {
            if (musicService?.playNext() == true) {
                updateCurrentSongUI()
                updateQueueUI()
            } else {
                Toast.makeText(this, "Fim da fila", Toast.LENGTH_SHORT).show()
            }
        }

        prevButton.setOnClickListener {
            if (musicService?.playPrevious() == true) {
                updateCurrentSongUI()
                updateQueueUI()
            } else {
                Toast.makeText(this, "Início da fila", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar SeekBar para arrastar
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = musicService?.getDuration() ?: 30000
                    val newPosition = (progress * duration) / 100
                    val time = formatTime(newPosition)
                    val totalTime = formatTime(duration)
                    timeText.text = "$time / $totalTime"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
                updateJob?.cancel()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                val progress = seekBar?.progress ?: 0
                val duration = musicService?.getDuration() ?: 30000
                val newPosition = (progress * duration) / 100
                musicService?.seekTo(newPosition)
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

        // Atualizar botões de navegação
        val playlistSize = musicService?.getQueueSize() ?: 0
        nextButton.isEnabled = playlistSize > 1
        prevButton.isEnabled = playlistSize > 0

        // COMENTE esta linha se não tiver queueSizeText
        // queueSizeText.text = "Fila: $playlistSize música${if (playlistSize != 1) "s" else ""}"

        // Mostrar/ocultar controles baseado no conteúdo
        val queueTitle = findViewById<TextView>(R.id.queue_title)
        if (playlistSize > 0) {
            queueTitle.visibility = View.VISIBLE
            queueRecyclerView.visibility = View.VISIBLE
            clearQueueButton.visibility = View.VISIBLE
        } else {
            queueTitle.visibility = View.GONE
            queueRecyclerView.visibility = View.GONE
            clearQueueButton.visibility = View.GONE
        }
    }

    private fun updateCurrentSongUI() {
        val currentSong = musicService?.getCurrentSong()
        if (currentSong != null) {
            currentSongTitle.text = currentSong.title
            currentSongArtist.text = currentSong.artist
            currentSongTitle.visibility = View.VISIBLE
            currentSongArtist.visibility = View.VISIBLE
        } else {
            currentSongTitle.visibility = View.GONE
            currentSongArtist.visibility = View.GONE
        }
        updateUI()
    }

    private fun updateQueueUI() {
        val playlist = musicService?.getPlaylist() ?: emptyList()
        val currentIndex = musicService?.getCurrentSongIndex() ?: -1

        queueAdapter.updateData(playlist, currentIndex)
        updateUI()
    }

    private fun updateRepeatButton(mode: MusicService.RepeatMode) {
        val (text, colorRes) = when (mode) {
            MusicService.RepeatMode.NONE -> Pair("REP", R.color.gray)
            MusicService.RepeatMode.ONE -> Pair("1", R.color.green)
            MusicService.RepeatMode.ALL -> Pair("ALL", R.color.blue)
        }

        repeatButton.text = text
        repeatButton.setBackgroundColor(ContextCompat.getColor(this, colorRes))
    }

    // COMENTE este método se não tiver botão shuffle
    // private fun updateShuffleButton(enabled: Boolean) {
    //     if (enabled) {
    //         shuffleButton.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_500))
    //         shuffleButton.text = "🔀 ON"
    //     } else {
    //         shuffleButton.setBackgroundColor(ContextCompat.getColor(this, R.color.gray))
    //         shuffleButton.text = "🔀 OFF"
    //     }
    // }

    private fun startUpdatingSeekBar() {
        updateJob?.cancel()
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isBound && isActive) {
                try {
                    if (!isUserSeeking) {
                        musicService?.let { service ->
                            val currentPos = service.getCurrentPosition()
                            val duration = service.getDuration()

                            if (duration > 0) {
                                val progress = (currentPos * 100) / duration
                                seekBar.progress = progress

                                val currentTime = formatTime(currentPos)
                                val totalTime = formatTime(duration)
                                timeText.text = "$currentTime / $totalTime"

                                // Atualizar título da música se necessário
                                if (currentPos == 0) {
                                    updateCurrentSongUI()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao atualizar seekbar: ${e.message}")
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

    override fun onResume() {
        super.onResume()
        updateUI()
        updateQueueUI()
        updateCurrentSongUI()
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

    //Necessario ajuste na comunicação do serviço
}