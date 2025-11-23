package com.example.getjob.data.websocket

import android.util.Log
import com.example.getjob.data.models.responses.MessageResponse
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

class ChatWebSocketClient(private val preferencesManager: PreferencesManager) {
    private var webSocket: WebSocket? = null
    private var currentJobId: Int? = null
    private var currentApplicationId: Int? = null
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val _messages = MutableStateFlow<List<MessageResponse>>(emptyList())
    val messages: StateFlow<List<MessageResponse>> = _messages.asStateFlow()
    
    // SharedFlow para emitir mensajes nuevos individuales (mejor para eventos únicos)
    private val _newMessageFlow = MutableSharedFlow<MessageResponse>(replay = 0, extraBufferCapacity = 1)
    val newMessageFlow: SharedFlow<MessageResponse> = _newMessageFlow.asSharedFlow()
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val gson = Gson()
    
    enum class ConnectionState {
        Connected,
        Disconnected,
        Connecting,
        Error
    }
    
    fun connect(jobId: Int, applicationId: Int? = null) {
        Log.d("ChatWebSocket", "🔌🔌🔌 CONNECT() LLAMADO - JobId: $jobId, ApplicationId: $applicationId")
        
        currentJobId = jobId
        currentApplicationId = applicationId
        if (webSocket != null) {
            Log.d("ChatWebSocket", "⚠️ WebSocket existente, desconectando primero...")
            disconnect()
        }
        
        // Cancelar reconexión anterior si existe
        reconnectJob?.cancel()
        reconnectJob = null
        
        _connectionState.value = ConnectionState.Connecting
        Log.d("ChatWebSocket", "🔌 Estado cambiado a: Connecting")
        
        val token = preferencesManager.getToken()
        if (token == null) {
            Log.e("ChatWebSocket", "❌❌❌ No hay token disponible - ABORTANDO CONEXIÓN")
            _connectionState.value = ConnectionState.Error
            return
        }
        Log.d("ChatWebSocket", "✅ Token disponible: ${token.take(20)}...")
        
        // Convertir HTTP a WebSocket (ws:// o wss://)
        val baseUrl = NetworkConfig.BASE_URL
        Log.d("ChatWebSocket", "🔌 BASE_URL original: $baseUrl")
        
        val wsUrl = baseUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://")
        Log.d("ChatWebSocket", "🔌 URL WebSocket convertida: $wsUrl")
        
        // Usar URL sin token (el token va en headers por seguridad)
        val fullUrl = buildString {
            append("$wsUrl/api/chat/ws/$jobId")
            if (currentApplicationId != null) {
                append("?application_id=${currentApplicationId}")
            }
        }
        Log.d("ChatWebSocket", "🔌🔌🔌 CONECTANDO A: $fullUrl")
        
        val request = Request.Builder()
            .url(fullUrl)
            .addHeader("ngrok-skip-browser-warning", "true")
            .addHeader("User-Agent", "ServiFast-Android-App/1.0")
            .addHeader("Authorization", "Bearer $token") // Token en headers, no en URL
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("ChatWebSocket", "✅✅✅ CONEXIÓN WEBSOCKET ABIERTA - Code: ${response.code}")
                Log.d("ChatWebSocket", "✅✅✅ URL: $fullUrl")
                Log.d("ChatWebSocket", "✅✅✅ JobId: $jobId, ApplicationId: $currentApplicationId")
                _connectionState.value = ConnectionState.Connected
                
                // Limpiar mensajes anteriores al reconectar
                clearMessages()
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("ChatWebSocket", "📨📨📨 MENSAJE WEBSOCKET RECIBIDO (RAW): $text")
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type", "")
                    
                    Log.d("ChatWebSocket", "🔍 Tipo de mensaje: '$type'")
                    
                    when (type) {
                        "message" -> {
                            val dataJson = json.optJSONObject("data")
                            if (dataJson != null) {
                                val message = gson.fromJson(dataJson.toString(), MessageResponse::class.java)
                                Log.d("ChatWebSocket", "✅✅✅ MENSAJE PARSEADO - ID: ${message.id}, Contenido: '${message.content}', De: ${message.sender_id}")
                                
                                // Emitir mensaje nuevo individual usando SharedFlow (PRIMERO, antes de agregar a lista)
                                scope.launch {
                                    try {
                                        _newMessageFlow.emit(message)
                                        Log.d("ChatWebSocket", "🚀🚀🚀 MENSAJE EMITIDO AL SHAREDFLOW - ID: ${message.id}")
                                    } catch (e: Exception) {
                                        Log.e("ChatWebSocket", "❌❌❌ ERROR AL EMITIR: ${e.message}", e)
                                    }
                                }
                                
                                // También agregar a la lista interna (para mantener historial)
                                addMessage(message)
                            } else {
                                Log.e("ChatWebSocket", "❌❌❌ El campo 'data' no existe o es null")
                                Log.e("ChatWebSocket", "JSON completo: $json")
                            }
                        }
                        "connected" -> {
                            Log.d("ChatWebSocket", "✅ Conectado al chat")
                        }
                        else -> {
                            Log.w("ChatWebSocket", "⚠️ Tipo desconocido: '$type'")
                            Log.w("ChatWebSocket", "JSON: $json")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatWebSocket", "❌❌❌ ERROR AL PROCESAR: ${e.message}", e)
                    Log.e("ChatWebSocket", "Texto: $text")
                }
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("ChatWebSocket", "Cerrando conexión: $code - $reason")
                webSocket.close(1000, null)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("ChatWebSocket", "Conexión cerrada: $code - $reason")
                _connectionState.value = ConnectionState.Disconnected
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("ChatWebSocket", "❌❌❌ ERROR EN WEBSOCKET: ${t.message}", t)
                Log.e("ChatWebSocket", "❌❌❌ Response Code: ${response?.code}")
                Log.e("ChatWebSocket", "❌❌❌ Response Message: ${response?.message}")
                Log.e("ChatWebSocket", "❌❌❌ URL intentada: $fullUrl")
                _connectionState.value = ConnectionState.Error
                
                // Evitar múltiples intentos de reconexión simultáneos
                if (currentJobId != null && _connectionState.value != ConnectionState.Connecting) {
                    // Cancelar reconexión anterior si existe
                    reconnectJob?.cancel()
                    
                    // Intentar reconectar después de 3 segundos usando coroutines
                    reconnectJob = scope.launch {
                        delay(3000)
                        if (_connectionState.value != ConnectionState.Connected && 
                            _connectionState.value != ConnectionState.Connecting && 
                            currentJobId != null) {
                            Log.d("ChatWebSocket", "🔄 Intentando reconectar...")
                            currentJobId?.let { connect(it, currentApplicationId) }
                        }
                    }
                }
            }
        })
    }
    
    fun sendMessage(content: String, hasImage: Boolean = false, imageUrl: String? = null) {
        val webSocket = this.webSocket
        if (webSocket == null) {
            Log.e("ChatWebSocket", "WebSocket es null")
            return
        }
        
        if (_connectionState.value == ConnectionState.Disconnected ||
            _connectionState.value == ConnectionState.Error) {
            Log.d("ChatWebSocket", "Reconectando WebSocket antes de enviar mensaje...")
            currentJobId?.let {
                connect(it, currentApplicationId)
            }
        }
        
        try {
            val message = JSONObject().apply {
                put("content", content)
                put("has_image", hasImage)
                if (imageUrl != null) {
                    put("image_url", imageUrl)
                }
            }
            val messageStr = message.toString()
            Log.d("ChatWebSocket", "Enviando mensaje: $messageStr")
            webSocket.send(messageStr)
            Log.d("ChatWebSocket", "✓ Mensaje enviado exitosamente")
        } catch (e: Exception) {
            Log.e("ChatWebSocket", "Error al enviar mensaje: ${e.message}", e)
        }
    }
    
    fun disconnect() {
        // Cancelar reconexión si existe
        reconnectJob?.cancel()
        reconnectJob = null
        
        webSocket?.close(1000, "Desconectado por el usuario")
        webSocket = null
        currentJobId = null
        currentApplicationId = null
        _connectionState.value = ConnectionState.Disconnected
    }
    
    /**
     * Limpia todos los recursos (llamar cuando se destruye el ViewModel)
     */
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
    
    fun addMessage(message: MessageResponse) {
        val currentMessages = _messages.value.toMutableList()
        // Solo agregar si no existe (evitar duplicados)
        if (!currentMessages.any { it.id == message.id }) {
            currentMessages.add(message)
            // Ordenar por fecha para mantener orden cronológico
            val sortedMessages = currentMessages.sortedBy { it.created_at }
            _messages.value = sortedMessages
            Log.d("ChatWebSocket", "✓ Mensaje agregado a StateFlow - ID: ${message.id}, Total: ${sortedMessages.size}")
        } else {
            Log.d("ChatWebSocket", "Mensaje duplicado ignorado en addMessage (ID: ${message.id})")
        }
    }
    
    /**
     * Agrega múltiples mensajes de una vez (útil para carga inicial)
     */
    fun addMessages(messages: List<MessageResponse>) {
        val currentMessages = _messages.value.toMutableList()
        val messageIds = currentMessages.map { it.id }.toSet()
        
        var addedCount = 0
        messages.forEach { message ->
            if (message.id !in messageIds) {
                currentMessages.add(message)
                addedCount++
            }
        }
        
        if (addedCount > 0) {
            // Ordenar por fecha
            val sortedMessages = currentMessages.sortedBy { it.created_at }
            _messages.value = sortedMessages
            Log.d("ChatWebSocket", "✓ $addedCount mensaje(s) agregado(s) a StateFlow. Total: ${sortedMessages.size}")
        }
    }
    
    fun clearMessages() {
        _messages.value = emptyList()
    }
    
}

