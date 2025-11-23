package com.example.getjob.data.api

import android.content.Context
import com.example.getjob.BuildConfig
import com.example.getjob.utils.NetworkConfig
import com.example.getjob.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var preferencesManager: PreferencesManager? = null
    
    fun initialize(context: Context) {
        preferencesManager = PreferencesManager(context)
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Solo loguear en modo debug para evitar exponer datos sensibles en producción
        // Usar BuildConfig.DEBUG en lugar de Log.isLoggable para que funcione correctamente
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    
    // Interceptor para evitar la página de advertencia de ngrok
    private val ngrokInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("ngrok-skip-browser-warning", "true")
            .addHeader("User-Agent", "ServiFast-Android-App/1.0")
            .build()
        chain.proceed(request)
    }
    
    // Interceptor para agregar token JWT (accede a preferencesManager de forma lazy)
    private val authInterceptor = okhttp3.Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        
        // Obtener el token fresco cada vez que se hace una petición
        val token = preferencesManager?.getToken()
        
        if (token != null && token.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        } else {
            // Si no hay token, puede ser que no se haya inicializado ApiClient o el usuario no está logueado
            android.util.Log.w("ApiClient", "No se encontró token de autenticación")
        }
        
        val response = chain.proceed(requestBuilder.build())
        
        // Manejar tokens expirados (401 Unauthorized o 403 Forbidden)
        if (response.code == 401 || response.code == 403) {
            android.util.Log.w("ApiClient", "Token expirado o inválido (${response.code}), limpiando sesión")
            // Limpiar datos de autenticación
            preferencesManager?.clearAuthData()
            // Emitir evento de token expirado para redirección automática
            CoroutineScope(Dispatchers.Main).launch {
                com.example.getjob.utils.AuthEventBus.emitTokenExpired()
            }
        }
        
        response
    }

    // Crear OkHttpClient de forma lazy para asegurar que preferencesManager esté inicializado
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(ngrokInterceptor)
            .addInterceptor(authInterceptor)  // Agregar interceptor de autenticación
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    // Crear Retrofit de forma lazy
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val workerApi: WorkerApi by lazy { retrofit.create(WorkerApi::class.java) }
    val jobApi: JobApi by lazy { retrofit.create(JobApi::class.java) }
    val commissionApi: CommissionApi by lazy { retrofit.create(CommissionApi::class.java) }
    val chatApi: ChatApi by lazy { retrofit.create(ChatApi::class.java) }
    val locationApi: LocationApi by lazy { retrofit.create(LocationApi::class.java) }
    val subscriptionApi: SubscriptionApi by lazy { retrofit.create(SubscriptionApi::class.java) }
    
    // OkHttpClient para WebSocket (sin Retrofit)
    val webSocketClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(ngrokInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

