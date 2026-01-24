package com.android.musicsplayerapp

import android.app.Service
import android.content.Intent
import android.os.IBinder

class YourService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        // Inicializar seu serviço
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Executar tarefa do serviço
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpar recursos
    }
}