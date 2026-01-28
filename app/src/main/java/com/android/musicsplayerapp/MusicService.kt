package com.android.musicsplayerapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var currentPosition = 0 // Armazena a posição atual
    private val channelId = "music_channel"
    private val notificationId = 1

    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MusicService", "✅ Service created")
        createNotificationChannel()
    }

    private fun initMediaPlayer() {
        try {
            // Criar MediaPlayer (sem arquivo real)
            mediaPlayer = MediaPlayer()
            mediaPlayer?.isLooping = true

            // Configurar listener
            mediaPlayer?.setOnCompletionListener {
                isPlaying = false
                currentPosition = 0 // Reset ao terminar
                updateNotification()
                Log.d("MusicService", "Música concluída")
            }

            Log.d("MusicService", "🎵 MediaPlayer inicializado")

        } catch (e: Exception) {
            Log.e("MusicService", "❌ Erro ao criar MediaPlayer: ${e.message}")
            mediaPlayer = MediaPlayer()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music player notifications"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MusicService", "🚀 onStartCommand")

        // Iniciar como foreground service
        startForeground(notificationId, buildNotification("Music Service"))

        intent?.action?.let { action ->
            Log.d("MusicService", "🎯 Ação: $action")
            when (action) {
                ACTION_PLAY -> playMusic()
                ACTION_PAUSE -> pauseMusic()
                ACTION_STOP -> stopMusic()
            }
        }

        return START_STICKY
    }

    fun playMusic() {
        Log.d("MusicService", "▶️ Play pressionado. Posição atual: $currentPosition")

        try {
            if (mediaPlayer == null) {
                initMediaPlayer()
            }

            mediaPlayer?.let { player ->
                if (!isPlaying) {
                    isPlaying = true

                    // Se estivermos em uma posição específica, continuar de lá
                    // (em um player real, usaríamos player.seekTo(currentPosition))

                    Log.d("MusicService", "🎶 Tocando a partir da posição: $currentPosition")

                    startForeground(notificationId, buildNotification("Playing Music"))
                }
            }
        } catch (e: Exception) {
            Log.e("MusicService", "❌ Erro: ${e.message}")
            isPlaying = false
        }
    }

    fun pauseMusic() {
        Log.d("MusicService", "⏸️ Pausando música. Posição atual: $currentPosition")

        // NÃO ZERAR currentPosition ao pausar!
        isPlaying = false
        Log.d("MusicService", "⏸️ Música pausada na posição: $currentPosition")

        updateNotification()
    }

    fun stopMusic() {
        Log.d("MusicService", "⏹️ Parando música")

        // Ao parar, ZERAR a posição
        isPlaying = false
        currentPosition = 0 // ⬅️ ISSO É O QUE VOCÊ QUER: reset ao parar
        Log.d("MusicService", "⏹️ Música parada, posição resetada")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
    }

    fun isMusicPlaying(): Boolean = isPlaying

    fun getCurrentPosition(): Int {
        // Se estiver tocando, incrementar a posição
        // Se estiver pausado, manter a posição atual
        if (isPlaying) {
            currentPosition += 1000 // Incrementa 1 segundo a cada chamada
            if (currentPosition > getDuration()) {
                currentPosition = 0 // Volta ao início se passar do fim
            }
        }
        return currentPosition
    }

    fun getDuration(): Int {
        // Duração fixa de 30 segundos para demonstração
        return 30000 // 30 segundos
    }

    // Método para definir posição específica (se quiser implementar arrastar seekbar)
    fun seekTo(position: Int) {
        currentPosition = position
        Log.d("MusicService", "🎯 Seek para posição: $position")
    }

    private fun buildNotification(title: String): Notification {
        val playIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_PLAY
        }
        val pauseIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_PAUSE
        }
        val stopIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_STOP
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val playPendingIntent = PendingIntent.getService(this, 0, playIntent, flags)
        val pausePendingIntent = PendingIntent.getService(this, 1, pauseIntent, flags)
        val stopPendingIntent = PendingIntent.getService(this, 2, stopIntent, flags)

        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent, flags
        )

        val playAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_play,
            "Play",
            playPendingIntent
        ).build()

        val pauseAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_pause,
            "Pause",
            pausePendingIntent
        ).build()

        val stopAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            stopPendingIntent
        ).build()

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(if (isPlaying) "Playing" else "Paused")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .addAction(if (isPlaying) pauseAction else playAction)
            .addAction(stopAction)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification() {
        val notification = buildNotification(
            if (isPlaying) "Playing Music" else "Music Paused"
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        Log.d("MusicService", "💀 Service destroyed")
    }

    companion object {
        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
    }
}