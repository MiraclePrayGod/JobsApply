package com.example.getjob

import android.app.Application
import com.example.getjob.data.api.ApiClient
import org.osmdroid.config.Configuration

class GetJobApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // ⚠️ CRÍTICO: Inicializar ApiClient ANTES de cualquier llamada a la API
        // Esto permite que el authInterceptor pueda leer el token desde PreferencesManager
        ApiClient.initialize(this)
        
        // Inicializar OSMDroid (requerido para que los mapas funcionen)
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName
        
        // Configuración adicional para OSMDroid
        Configuration.getInstance().osmdroidBasePath = cacheDir
        Configuration.getInstance().osmdroidTileCache = cacheDir
    }
}

