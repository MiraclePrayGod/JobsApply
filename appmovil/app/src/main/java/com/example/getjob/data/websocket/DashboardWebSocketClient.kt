package com.example.getjob.data.websocket

import android.util.Log
import com.example.getjob.utils.NetworkConfig
import com.example.getjob.utils.PreferencesManager
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cliente WebSocket para notificaciones del dashboard
 * Escucha notificaciones en tiempo real de nuevos mensajes, aplicaciones, etc.
 */
class DashboardWebSocketClient(private val preferencesManager: PreferencesManager) {
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var consecutiveFailures = 0 // Contador de fallos consecutivos
    private var lastFailureTime = 0L // Timestamp del último fallo
    private val maxConsecutiveFailures = 3 // Máximo de intentos antes de parar
    private val minReconnectDelay = 5000L // 5 segundos mínimo
    private val maxReconnectDelay = 60000L // 60 segundos máximo (backoff exponencial)
    // Usar Dispatchers.IO para emitir notificaciones inmediatamente sin bloquear el hilo principal
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // SharedFlow para emitir notificaciones nuevas
    private val _notificationFlow = MutableSharedFlow<DashboardNotification>(replay = 0, extraBufferCapacity = 10)
    val notificationFlow: SharedFlow<DashboardNotification> = _notificationFlow.asSharedFlow()
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val gson = Gson()
    
    enum class ConnectionState {
        Connected,
        Disconnected,
        Connecting,
        Error
    }
    
    data class DashboardNotification(
        val type: String, // "new_message", "new_application", "job_status_changed", etc.
        val data: Map<String, Any?>
    )
    
    fun connect() {
        Log.d("DashboardWebSocket", "🔌 CONNECT() LLAMADO")
        
        // Verificar token antes de intentar conectar
        val token = preferencesManager.getToken()
        if (token == null || token.isEmpty()) {
            Log.e("DashboardWebSocket", "❌ No hay token disponible - ABORTANDO CONEXIÓN")
            _connectionState.value = ConnectionState.Error
            return
        }
        
        if (webSocket != null) {
            Log.d("DashboardWebSocket", "⚠️ WebSocket existente, desconectando primero...")
            disconnect()
        }
        
        // Cancelar reconexión anterior si existe
        reconnectJob?.cancel()
        reconnectJob = null
        
        Log.d("DashboardWebSocket", "✅ Token disponible: ${token.take(20)}...")
        
        // Convertir HTTP a WebSocket (ws:// o wss://)
        val baseUrl = NetworkConfig.BASE_URL
        val wsUrl = baseUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://")
        
        val fullUrl = "$wsUrl/api/notifications/ws/dashboard"
        Log.d("DashboardWebSocket", "🔌 CONECTANDO A: $fullUrl")
        
        val request = Request.Builder()
            .url(fullUrl)
            .addHeader("ngrok-skip-browser-warning", "true")
            .addHeader("User-Agent", "ServiFast-Android-App/1.0")
            .addHeader("Authorization", "Bearer $token")
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("DashboardWebSocket", "✅✅✅ CONEXIÓN WEBSOCKET ABIERTA - Code: ${response.code}")
                _connectionState.value = ConnectionState.Connected
                // Resetear contador de fallos al conectar exitosamente
                consecutiveFailures = 0
                lastFailureTime = 0L
                
                // Enviar ping periódico para mantener la conexión viva
                startPingPong()
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("DashboardWebSocket", "📨 MENSAJE WEBSOCKET RECIBIDO: $text")
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type", "")
                    
                    when (type) {
                        "connected" -> {
                            Log.d("DashboardWebSocket", "✅ Conectado al dashboard")
                        }
                        "pong" -> {
                            Log.d("DashboardWebSocket", "🏓 Pong recibido")
                        }
                        "new_message" -> {
                            val dataJson = json.optJSONObject("data")
                            if (dataJson != null) {
                                val dataMap = jsonObjectToMap(dataJson)
                                val notification = DashboardNotification("new_message", dataMap)
                                // Emitir inmediatamente sin delay
                                scope.launch(Dispatchers.IO) {
                                    _notificationFlow.emit(notification)
                                    Log.d("DashboardWebSocket", "🚀⚡ Notificación 'new_message' emitida INMEDIATAMENTE")
                                }
                            } else {
                                Log.w("DashboardWebSocket", "⚠️ Notificación 'new_message' sin data")
                            }
                        }
                        "new_application" -> {
                            val dataJson = json.optJSONObject("data")
                            if (dataJson != null) {
                                val dataMap = jsonObjectToMap(dataJson)
                                val notification = DashboardNotification("new_application", dataMap)
                                scope.launch {
                                    _notificationFlow.emit(notification)
                                    Log.d("DashboardWebSocket", "🚀 Notificación 'new_application' emitida")
                                }
                            }
                        }
                        else -> {
                            Log.w("DashboardWebSocket", "⚠️ Tipo de notificación desconocido: '$type'")
                            // Emitir de todas formas por si acaso
                            val dataJson = json.optJSONObject("data")
                            if (dataJson != null) {
                                val dataMap = jsonObjectToMap(dataJson)
                                val notification = DashboardNotification(type, dataMap)
                                scope.launch {
                                    _notificationFlow.emit(notification)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DashboardWebSocket", "❌ ERROR AL PROCESAR: ${e.message}", e)
                }
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("DashboardWebSocket", "Cerrando conexión: $code - $reason")
                webSocket.close(1000, null)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("DashboardWebSocket", "Conexión cerrada: $code - $reason")
                _connectionState.value = ConnectionState.Disconnected
                stopPingPong()
                
                // Intentar reconectar después de 5 segundos
                reconnectJob = scope.launch {
                    delay(5000)
                    if (_connectionState.value == ConnectionState.Disconnected) {
                        Log.d("DashboardWebSocket", "🔄 Intentando reconectar...")
                        connect()
                    }
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val responseCode = response?.code
                val is403Forbidden = responseCode == 403 || 
                    (t.message?.contains("403", ignoreCase = true) == true) ||
                    (t.message?.contains("Forbidden", ignoreCase = true) == true)
                
                Log.e("DashboardWebSocket", "❌ ERROR EN WEBSOCKET: ${t.message}", t)
                if (responseCode != null) {
                    Log.e("DashboardWebSocket", "📊 Response Code: $responseCode")
                }
                
                _connectionState.value = ConnectionState.Error
                stopPingPong()
                
                consecutiveFailures++
                lastFailureTime = System.currentTimeMillis()
                
                // Si es 403 Forbidden, no reconectar (token inválido/expirado)
                if (is403Forbidden) {
                    Log.w("DashboardWebSocket", "🚫 Error 403 Forbidden - Token inválido/expirado. NO se intentará reconectar.")
                    Log.w("DashboardWebSocket", "💡 El usuario debe hacer login nuevamente.")
                    // Limpiar el WebSocket
                    webSocket.close(1000, "403 Forbidden - Token inválido")
                    this@DashboardWebSocketClient.webSocket = null
                    return
                }
                
                // Si hay demasiados fallos consecutivos, parar de intentar
                if (consecutiveFailures >= maxConsecutiveFailures) {
                    Log.w("DashboardWebSocket", "⚠️ Demasiados fallos consecutivos ($consecutiveFailures). Parando reconexión automática.")
                    Log.w("DashboardWebSocket", "💡 La reconexión se intentará cuando el usuario vuelva a la pantalla.")
                    return
                }
                
                // Verificar que el token exista antes de reconectar
                val token = preferencesManager.getToken()
                if (token == null || token.isEmpty()) {
                    Log.w("DashboardWebSocket", "🚫 No hay token disponible. NO se intentará reconectar.")
                    return
                }
                
                // Calcular delay con backoff exponencial
                val delay = minOf(
                    minReconnectDelay * (1 shl (consecutiveFailures - 1)), // Backoff exponencial
                    maxReconnectDelay
                )
                
                Log.d("DashboardWebSocket", "⏱️ Esperando ${delay}ms antes de reconectar (intento $consecutiveFailures/$maxConsecutiveFailures)")
                
                // Intentar reconectar con delay exponencial
                reconnectJob = scope.launch {
                    delay(delay)
                    // Verificar nuevamente el token antes de reconectar
                    val currentToken = preferencesManager.getToken()
                    if (currentToken == null || currentToken.isEmpty()) {
                        Log.w("DashboardWebSocket", "🚫 Token no disponible al intentar reconectar. Cancelando.")
                        return@launch
                    }
                    
                    if (_connectionState.value == ConnectionState.Error || 
                        _connectionState.value == ConnectionState.Disconnected) {
                        Log.d("DashboardWebSocket", "🔄 Intentando reconectar después de error...")
                        connect()
                    }
                }
            }
        })
    }
    
    private var pingJob: Job? = null
    
    private fun startPingPong() {
        stopPingPong()
        pingJob = scope.launch {
            while (_connectionState.value == ConnectionState.Connected) {
                delay(30000) // Ping cada 30 segundos
                try {
                    webSocket?.send("ping")
                    Log.d("DashboardWebSocket", "🏓 Ping enviado")
                } catch (e: Exception) {
                    Log.e("DashboardWebSocket", "❌ Error al enviar ping: ${e.message}")
                }
            }
        }
    }
    
    private fun stopPingPong() {
        pingJob?.cancel()
        pingJob = null
    }
    
    fun disconnect() {
        Log.d("DashboardWebSocket", "Desconectando WebSocket...")
        stopPingPong()
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "Desconectado por el usuario")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
        // Resetear contador al desconectar manualmente
        consecutiveFailures = 0
        lastFailureTime = 0L
    }
    
    /**
     * Resetea el contador de fallos y permite reconexión
     * Útil después de un login exitoso o cuando el usuario vuelve a la pantalla
     */
    fun resetFailureCount() {
        Log.d("DashboardWebSocket", "🔄 Reseteando contador de fallos")
        consecutiveFailures = 0
        lastFailureTime = 0L
    }
    
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
    
    private fun jsonObjectToMap(json: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.opt(key)
            map[key] = when (value) {
                is JSONObject -> jsonObjectToMap(value)
                else -> value
            }
        }
        return map
    }
}

