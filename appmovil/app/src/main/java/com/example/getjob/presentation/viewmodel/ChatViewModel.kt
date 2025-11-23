package com.example.getjob.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.getjob.data.models.responses.MessageResponse
import com.example.getjob.data.models.responses.JobApplicationResponse
import com.example.getjob.data.repository.ChatRepository
import com.example.getjob.data.repository.JobRepository
import com.example.getjob.data.websocket.ChatWebSocketClient
import com.example.getjob.utils.ImageStorageManager
import com.example.getjob.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<MessageResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val errorMessage: String? = null,
    val messageText: String = "",
    val application: JobApplicationResponse? = null, // Información de la aplicación (trabajador)
    val isLoadingApplication: Boolean = false,
    val isAcceptingWorker: Boolean = false
)

class ChatViewModel(
    application: Application,
    private val jobId: Int,
    private val applicationId: Int? = null
) : AndroidViewModel(application) {
    private val chatRepository = ChatRepository()
    private val jobRepository = JobRepository()
    private val preferencesManager = PreferencesManager(application)
    private val webSocketClient = ChatWebSocketClient(preferencesManager)
    private val imageStorageManager = ImageStorageManager(application)
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    init {
        android.util.Log.d("ChatViewModel", "🚀🚀🚀 INICIALIZANDO ChatViewModel - JobId: $jobId, ApplicationId: $applicationId")
        
        // Conectar WebSocket inmediatamente
        viewModelScope.launch {
            android.util.Log.d("ChatViewModel", "🔌 Llamando a connectWebSocket()...")
            connectWebSocket()
        }
        loadMessages()
        if (applicationId != null) {
            loadApplication()
        }
        cleanExpiredImages()
        
        // Observar mensajes nuevos del WebSocket - ENFOQUE DIRECTO CON SharedFlow
        // Cuando llega un mensaje nuevo, agregarlo inmediatamente al UI
        viewModelScope.launch {
            android.util.Log.d("ChatViewModel", "🔵 INICIANDO OBSERVACIÓN DEL SharedFlow...")
            webSocketClient.newMessageFlow.collect { message ->
                android.util.Log.d("ChatViewModel", "🔥🔥🔥🔥🔥 MENSAJE NUEVO RECIBIDO DEL SHAREDFLOW - ID: ${message.id}, De: ${message.sender_id}, Contenido: '${message.content}'")
                
                // Agregar mensaje directamente al estado actual
                val currentMessages = _uiState.value.messages.toMutableList()
                
                // Solo agregar si no existe (evitar duplicados)
                if (!currentMessages.any { it.id == message.id }) {
                    currentMessages.add(message)
                    
                    // Ordenar por fecha
                    val sortedMessages = currentMessages.sortedBy { it.created_at }
                    
                    // Actualizar UI inmediatamente
                    _uiState.value = _uiState.value.copy(
                        messages = sortedMessages
                    )
                    
                    android.util.Log.d("ChatViewModel", "✅✅✅✅✅ UI ACTUALIZADA - Mensaje agregado. Total: ${sortedMessages.size}")
                } else {
                    android.util.Log.d("ChatViewModel", "⚠️ Mensaje duplicado ignorado - ID: ${message.id}")
                }
            }
        }
        
        // Observar estado de conexión
        viewModelScope.launch {
            webSocketClient.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    isConnected = state == ChatWebSocketClient.ConnectionState.Connected
                )
            }
        }
        
        // POLLING COMO FALLBACK: Recargar mensajes cada 3 segundos si el WebSocket no está funcionando
        // Esto asegura que los mensajes nuevos aparezcan aunque el WebSocket falle
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(3000) // Esperar 3 segundos
                
                // Solo hacer polling si hay mensajes cargados (para no hacer polling innecesario al inicio)
                if (_uiState.value.messages.isNotEmpty()) {
                    val lastMessageId = _uiState.value.messages.maxOfOrNull { it.id } ?: 0
                    
                    // Cargar mensajes y verificar si hay nuevos
                    chatRepository.getMessages(jobId, applicationId)
                        .onSuccess { newMessages ->
                            val currentMessageIds = _uiState.value.messages.map { it.id }.toSet()
                            val newMessageIds = newMessages.map { it.id }.toSet()
                            
                            // Si hay mensajes nuevos que no están en la lista actual
                            val hasNewMessages = newMessageIds.any { it !in currentMessageIds }
                            
                            if (hasNewMessages) {
                                android.util.Log.d("ChatViewModel", "🔄 Polling detectó mensajes nuevos, actualizando UI...")
                                _uiState.value = _uiState.value.copy(
                                    messages = newMessages.sortedBy { it.created_at }
                                )
                            }
                        }
                        .onFailure { 
                            // Silenciar errores de polling para no spamear logs
                        }
                }
            }
        }
    }
    
    private fun connectWebSocket() {
        viewModelScope.launch {
            android.util.Log.d("ChatViewModel", "🔌 Intentando conectar WebSocket...")
            // Intentar conectar inmediatamente
            webSocketClient.connect(jobId, applicationId)
            
            // Esperar un poco y verificar estado
            kotlinx.coroutines.delay(2000)
            val state = webSocketClient.connectionState.value
            android.util.Log.d("ChatViewModel", "🔌 Estado WebSocket después de 2s: $state")
            
            // Si no se conecta en 3 segundos, intentar de nuevo
            if (state != ChatWebSocketClient.ConnectionState.Connected) {
                android.util.Log.d("ChatViewModel", "🔄 Reintentando conexión WebSocket...")
                webSocketClient.connect(jobId, applicationId)
                
                kotlinx.coroutines.delay(2000)
                val newState = webSocketClient.connectionState.value
                android.util.Log.d("ChatViewModel", "🔌 Estado WebSocket después de reintento: $newState")
            } else {
                android.util.Log.d("ChatViewModel", "✅ WebSocket conectado exitosamente")
            }
        }
    }
    
    private fun loadMessages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            chatRepository.getMessages(jobId, applicationId)
                .onSuccess { messages ->
                    // Actualizar directamente el estado con los mensajes cargados
                    // No limpiar el WebSocket client para evitar perder mensajes nuevos
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        messages = messages.sortedBy { it.created_at }
                    )
                    android.util.Log.d("ChatViewModel", "Mensajes iniciales cargados: ${messages.size}")
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Error al cargar mensajes"
                    )
                }
        }
    }
    
    fun sendMessage(content: String, hasImage: Boolean = false, imageUrl: String? = null) {
        if (content.isBlank() && !hasImage && imageUrl == null) return
        
        viewModelScope.launch {
            android.util.Log.d("ChatViewModel", "📤 Enviando mensaje por REST...")
            
            // Enviar por REST API
            chatRepository.sendMessage(jobId, content, hasImage, imageUrl, applicationId)
                .onSuccess { message ->
                    android.util.Log.d("ChatViewModel", "✅ Mensaje enviado exitosamente - ID: ${message.id}")
                    
                    // Agregar mensaje localmente como fallback (feedback inmediato)
                    // Si el WebSocket funciona, el mensaje llegará y se agregará de nuevo (pero se detectará como duplicado)
                    val currentMessages = _uiState.value.messages.toMutableList()
                    if (!currentMessages.any { it.id == message.id }) {
                        currentMessages.add(message)
                        val sortedMessages = currentMessages.sortedBy { it.created_at }
                        _uiState.value = _uiState.value.copy(messages = sortedMessages)
                        android.util.Log.d("ChatViewModel", "✓ Mensaje agregado localmente (fallback) - ID: ${message.id}")
                    }
                    
                    // El backend enviará el mensaje a todos los WebSocket conectados
                    // Si el WebSocket está conectado, el mensaje llegará y se actualizará
                    android.util.Log.d("ChatViewModel", "⏳ Esperando confirmación por WebSocket...")
                }
                .onFailure { exception ->
                    android.util.Log.e("ChatViewModel", "❌ Error al enviar mensaje: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        errorMessage = exception.message ?: "Error al enviar mensaje"
                    )
                }
        }
    }
    
    fun updateMessageText(text: String) {
        _uiState.value = _uiState.value.copy(messageText = text)
    }
    
    fun sendImage(base64Image: String) {
        viewModelScope.launch {
            // Convertir base64 a data URL para enviar al backend
            val imageUrl = "data:image/jpeg;base64,$base64Image"
            
            // Enviar mensaje con imagen (el backend guardará la URL)
            sendMessage("", hasImage = true, imageUrl = imageUrl)
            
            // También guardar imagen localmente para acceso rápido
            kotlinx.coroutines.delay(500) // Esperar a que se cree el mensaje
            val lastMessage = _uiState.value.messages.lastOrNull()
            if (lastMessage != null) {
                imageStorageManager.saveImage(base64Image, lastMessage.id)
            }
        }
    }
    
    suspend fun getImageForMessage(messageId: Int, fileName: String?): android.graphics.Bitmap? {
        return if (fileName != null) {
            imageStorageManager.getImage(fileName)
        } else {
            null
        }
    }
    
    private fun cleanExpiredImages() {
        viewModelScope.launch {
            imageStorageManager.cleanExpiredImages()
        }
    }
    
    private fun loadApplication() {
        if (applicationId == null) return
        
        // Solo los clientes necesitan cargar la info completa de la aplicación
        val userRole = preferencesManager.getUserRole()
        if (userRole != "client") {
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingApplication = true)
            
            jobRepository.getJobApplications(jobId)
                .onSuccess { applications ->
                    val app = applications.firstOrNull { it.id == applicationId }
                    _uiState.value = _uiState.value.copy(
                        isLoadingApplication = false,
                        application = app
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingApplication = false,
                        errorMessage = exception.message ?: "Error al cargar información del trabajador"
                    )
                }
        }
    }
    
    fun acceptWorker(onSuccess: () -> Unit) {
        android.util.Log.d("ChatViewModel", "🔵 acceptWorker() llamado - jobId: $jobId, applicationId: $applicationId")
        
        if (applicationId == null) {
            android.util.Log.w("ChatViewModel", "❌ applicationId es null, abortando")
            return
        }
        
        viewModelScope.launch {
            android.util.Log.d("ChatViewModel", "⏳ Iniciando aceptación de trabajador...")
            _uiState.value = _uiState.value.copy(isAcceptingWorker = true, errorMessage = null)
            
            jobRepository.acceptWorker(jobId, applicationId)
                .onSuccess { jobResponse ->
                    android.util.Log.d("ChatViewModel", "✅ Trabajador aceptado exitosamente - Job ID: ${jobResponse.id}, Status: ${jobResponse.status}")
                    _uiState.value = _uiState.value.copy(
                        isAcceptingWorker = false,
                        application = _uiState.value.application?.copy(is_accepted = true)
                    )
                    android.util.Log.d("ChatViewModel", "🔄 Llamando callback onSuccess()")
                    onSuccess()
                }
                .onFailure { exception ->
                    android.util.Log.e("ChatViewModel", "❌ Error al aceptar trabajador: ${exception.message}", exception)
                    
                    // Mensaje personalizado y amigable para el usuario
                    val userFriendlyMessage = when {
                        exception.message?.contains("ya tiene un trabajo activo", ignoreCase = true) == true ||
                        exception.message?.contains("trabajador ya tiene", ignoreCase = true) == true ||
                        exception.message?.contains("already has an active job", ignoreCase = true) == true -> {
                            "Este trabajador está ocupado. Acepte al trabajador cuando le indique que ya terminó con el servicio en curso que está teniendo."
                        }
                        else -> {
                            exception.message ?: "Error al aceptar trabajador"
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isAcceptingWorker = false,
                        errorMessage = userFriendlyMessage
                    )
                }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        webSocketClient.cleanup()
    }
}

