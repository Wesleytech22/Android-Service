package com.android.musicsplayerapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat

class MusicService : Service() {

    private var isPlaying = false
    private var currentPosition = 0
    private val playlist = mutableListOf<Song>()
    private var currentSongIndex = -1
    private var repeatMode = RepeatMode.NONE
    private val channelId = "music_channel"
    private val notificationId = 1

    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    private val binder = LocalBinder()

    enum class RepeatMode {
        NONE, ONE, ALL
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.d("MusicService", "✅ Service created")
        createNotificationChannel()
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
        startForeground(notificationId, buildNotification("Music Player"))

        intent?.action?.let { action ->
            when (action) {
                ACTION_PLAY -> playMusic()
                ACTION_PAUSE -> pauseMusic()
                ACTION_STOP -> stopMusic()
                ACTION_NEXT -> playNext()
                ACTION_PREVIOUS -> playPrevious()
            }
        }

        return START_STICKY
    }

    // ==================== MÉTODOS DA FILA ====================

    fun addToQueue(song: Song) {
        playlist.add(song)
        if (currentSongIndex == -1 && playlist.isNotEmpty()) {
            currentSongIndex = 0
        }
        updateNotification()
        Log.d("MusicService", "➕ Adicionada à fila: ${song.title}")
    }

    fun addToQueue(songs: List<Song>) {
        playlist.addAll(songs)
        if (currentSongIndex == -1 && playlist.isNotEmpty()) {
            currentSongIndex = 0
        }
        updateNotification()
        Log.d("MusicService", "➕ Adicionadas ${songs.size} músicas à fila")
    }

    fun addNext(song: Song) {
        if (playlist.isEmpty()) {
            playlist.add(song)
            currentSongIndex = 0
        } else {
            val nextIndex = currentSongIndex + 1
            if (nextIndex < playlist.size) {
                playlist.add(nextIndex, song)
            } else {
                playlist.add(song)
            }
        }
        updateNotification()
        Log.d("MusicService", "⏭️ Adicionada como próxima: ${song.title}")
    }

    fun removeFromQueue(index: Int): Boolean {
        if (index < 0 || index >= playlist.size) return false

        val removedSong = playlist[index]
        playlist.removeAt(index)

        when {
            playlist.isEmpty() -> {
                currentSongIndex = -1
                stopMusic()
            }
            index < currentSongIndex -> currentSongIndex--
            index == currentSongIndex -> {
                if (isPlaying) {
                    playNext()
                } else if (playlist.isNotEmpty()) {
                    currentSongIndex = if (currentSongIndex >= playlist.size) 0 else currentSongIndex
                }
            }
        }

        updateNotification()
        Log.d("MusicService", "➖ Removida da fila: ${removedSong.title}")
        return true
    }

    private fun onSongCompletion() {
        when (repeatMode) {
            RepeatMode.ONE -> {
                currentPosition = 0
                Log.d("MusicService", "🔂 Repetindo mesma música")
                return
            }
            RepeatMode.ALL -> {
                if (playNext()) {
                    Log.d("MusicService", "🔁 Avançando para próxima (modo ALL)")
                    return
                }
            }
            RepeatMode.NONE -> {
                if (playNext()) {
                    Log.d("MusicService", "⏭️ Avançando para próxima")
                    return
                }
            }
        }

        isPlaying = false
        currentPosition = 0
        updateNotification()
        Log.d("MusicService", "🏁 Fila concluída")
    }

    fun playMusic() {
        if (playlist.isEmpty()) {
            Log.d("MusicService", "⚠️ Nenhuma música na fila")
            return
        }

        if (currentSongIndex == -1) {
            currentSongIndex = 0
        }

        if (!isPlaying) {
            isPlaying = true
            updateNotification()
            Log.d("MusicService", "▶️ Iniciando reprodução: ${getCurrentSong()?.title}")
        }
    }

    fun pauseMusic() {
        isPlaying = false
        updateNotification()
        Log.d("MusicService", "⏸️ Música pausada")
    }

    fun stopMusic() {
        isPlaying = false
        currentPosition = 0
        updateNotification()
        Log.d("MusicService", "⏹️ Música parada")
    }

    fun playNext(): Boolean {
        if (playlist.isEmpty()) return false

        var nextIndex = currentSongIndex + 1

        if (nextIndex >= playlist.size) {
            if (repeatMode == RepeatMode.ALL) {
                nextIndex = 0
            } else {
                return false
            }
        }

        if (nextIndex != currentSongIndex) {
            currentSongIndex = nextIndex
            updateNotification()
            Log.d("MusicService", "⏭️ Trocando para próxima música")
            return true
        }

        return false
    }

    fun playPrevious(): Boolean {
        if (playlist.isEmpty()) return false

        var prevIndex = currentSongIndex - 1

        if (prevIndex < 0) {
            if (repeatMode == RepeatMode.ALL) {
                prevIndex = playlist.size - 1
            } else {
                return false
            }
        }

        if (prevIndex != currentSongIndex) {
            currentSongIndex = prevIndex
            updateNotification()
            Log.d("MusicService", "⏮️ Voltando para música anterior")
            return true
        }

        return false
    }

    // ==================== MÉTODOS DE INFORMAÇÃO ====================

    fun getPlaylist(): List<Song> = playlist.toList()

    fun getCurrentSong(): Song? {
        return if (currentSongIndex in 0 until playlist.size) {
            playlist[currentSongIndex]
        } else {
            null
        }
    }

    fun getCurrentSongIndex(): Int = currentSongIndex

    fun getRepeatMode(): RepeatMode = repeatMode

    fun getQueueSize(): Int = playlist.size

    fun clearQueue() {
        playlist.clear()
        currentSongIndex = -1
        stopMusic()
        updateNotification()
        Log.d("MusicService", "🗑️ Fila limpa")
    }

    fun setRepeatMode(mode: RepeatMode) {
        repeatMode = mode
        updateNotification()
        Log.d("MusicService", "🔄 Modo repetição: $mode")
    }

    fun isMusicPlaying(): Boolean = isPlaying

    fun getCurrentPosition(): Int {
        if (isPlaying) {
            currentPosition += 1000
            val duration = getDuration()
            if (currentPosition > duration) {
                currentPosition = 0
                onSongCompletion()
            }
        }
        return currentPosition
    }

    fun getDuration(): Int {
        return getCurrentSong()?.duration ?: 30000
    }

    fun seekTo(position: Int) {
        currentPosition = position.coerceIn(0, getDuration())
        Log.d("MusicService", "🎯 Seek para posição: $currentPosition")
    }

    // ==================== NOTIFICAÇÃO ====================

    private fun buildNotification(title: String): Notification {
        // Intents para os botões
        val playIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_PLAY
        }
        val pauseIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_PAUSE
        }
        val stopIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_STOP
        }
        val nextIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_NEXT
        }
        val prevIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_PREVIOUS
        }

        // CORREÇÃO: Flags do PendingIntent
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val playPendingIntent = PendingIntent.getService(this, 0, playIntent, flags)
        val pausePendingIntent = PendingIntent.getService(this, 1, pauseIntent, flags)
        val stopPendingIntent = PendingIntent.getService(this, 2, stopIntent, flags)
        val nextPendingIntent = PendingIntent.getService(this, 3, nextIntent, flags)
        val prevPendingIntent = PendingIntent.getService(this, 4, prevIntent, flags)

        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent, flags
        )

        val currentSong = getCurrentSong()
        val notificationTitle = currentSong?.title ?: "Music Player"
        val notificationText = if (currentSong != null) {
            "${currentSong.artist} • ${playlist.size} na fila"
        } else {
            "Nenhuma música na fila"
        }

        // Criar ações da notificação
        val prevAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_previous,
            "Anterior",
            prevPendingIntent
        ).build()

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause,
                "Pausar",
                pausePendingIntent
            ).build()
        } else {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play,
                "Tocar",
                playPendingIntent
            ).build()
        }

        val nextAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_next,
            "Próxima",
            nextPendingIntent
        ).build()

        val stopAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Parar",
            stopPendingIntent
        ).build()

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(stopAction)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2))
            .build()
    }

    private fun updateNotification() {
        try {
            val notification = buildNotification(
                if (isPlaying) "Tocando" else "Pausado"
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notification)
        } catch (e: Exception) {
            Log.e("MusicService", "Erro ao atualizar notificação: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MusicService", "💀 Service destroyed")
    }

    companion object {
        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREVIOUS = "ACTION_PREVIOUS"
    }
}