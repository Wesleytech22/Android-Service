package com.android.musicsplayerapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class MusicService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MusicService", "✅ Serviço criado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MusicService", "✅ Serviço iniciado e rodando")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MusicService", "✅ Serviço parado")
    }
}