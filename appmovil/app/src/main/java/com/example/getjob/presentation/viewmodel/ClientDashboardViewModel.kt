package com.example.getjob.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.getjob.data.models.responses.JobResponse
import com.example.getjob.data.models.responses.JobApplicationResponse
import com.example.getjob.data.repository.JobRepository
import com.example.getjob.data.websocket.DashboardWebSocketClient
import com.example.getjob.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

data class JobWithApplications(
    val job: JobResponse,
    val applications: List<JobApplicationResponse> = emptyList()
)

data class ClientDashboardUiState(
    val isLoading: Boolean = false,
    val jobsWithApplications: List<JobWithApplications> = emptyList(),
    val errorMessage: String? = null,
    val isWebSocketConnected: Boolean = false
)

class ClientDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val jobRepository = JobRepository()
    private val preferencesManager = PreferencesManager(application)
    private val webSocketClient = DashboardWebSocketClient(preferencesManager)
    
    private val _uiState = MutableStateFlow(ClientDashboardUiState())
    val uiState: StateFlow<ClientDashboardUiState> = _uiState.asStateFlow()
    
    init {
        Log.d("ClientDashboardViewModel", "🚀 Inicializando ClientDashboardViewModel")
        loadMyJobs()
        connectWebSocket()
        observeNotifications()
    }
    
    private fun connectWebSocket() {
        viewModelScope.launch {
            try {
                Log.d("ClientDashboardViewModel", "🔌 Conectando WebSocket para dashboard...")
                // Resetear contador de fallos antes de conectar
                webSocketClient.resetFailureCount()
                webSocketClient.connect()
                
                // Observar estado de conexión
                webSocketClient.connectionState.collect { state ->
                    val isConnected = state == DashboardWebSocketClient.ConnectionState.Connected
                    _uiState.value = _uiState.value.copy(isWebSocketConnected = isConnected)
                    Log.d("ClientDashboardViewModel", "🔌 Estado WebSocket: $state (connected=$isConnected)")
                }
            } catch (e: Exception) {
                Log.e("ClientDashboardViewModel", "❌ Error al conectar WebSocket: ${e.message}", e)
            }
        }
    }
    
    private fun observeNotifications() {
        viewModelScope.launch {
            webSocketClient.notificationFlow.collect { notification ->
                Log.d("ClientDashboardViewModel", "📬 Notificación recibida: ${notification.type}")
                
                when (notification.type) {
                    "new_message" -> {
                        // Actualizar solo el trabajo específico que recibió el mensaje
                        val jobIdValue = notification.data["job_id"]
                        val jobId = when (jobIdValue) {
                            is Int -> jobIdValue
                            is Number -> jobIdValue.toInt()
                            is String -> jobIdValue.toIntOrNull()
                            else -> null
                        }
                        if (jobId != null) {
                            Log.d("ClientDashboardViewModel", "⚡ Actualizando trabajo $jobId inmediatamente...")
                            updateSingleJob(jobId)
                        } else {
                            // Fallback: refrescar todos si no tenemos job_id
                            Log.d("ClientDashboardViewModel", "🔄 Nuevo mensaje detectado, refrescando todos los trabajos...")
                            refreshJobs()
                        }
                    }
                    "new_application" -> {
                        // Actualizar solo el trabajo específico
                        val jobIdValue = notification.data["job_id"]
                        val jobId = when (jobIdValue) {
                            is Int -> jobIdValue
                            is Number -> jobIdValue.toInt()
                            is String -> jobIdValue.toIntOrNull()
                            else -> null
                        }
                        if (jobId != null) {
                            Log.d("ClientDashboardViewModel", "⚡ Actualizando trabajo $jobId inmediatamente...")
                            updateSingleJob(jobId)
                        } else {
                            Log.d("ClientDashboardViewModel", "🔄 Nueva aplicación detectada, refrescando trabajos...")
                            refreshJobs()
                        }
                    }
                    else -> {
                        Log.d("ClientDashboardViewModel", "🔄 Notificación '${notification.type}' recibida, refrescando trabajos...")
                        refreshJobs()
                    }
                }
            }
        }
    }
    
    /**
     * Actualiza un solo trabajo en lugar de recargar todos.
     * Esto es mucho más rápido y eficiente.
     */
    private fun updateSingleJob(jobId: Int) {
        viewModelScope.launch {
            try {
                // Cargar el trabajo específico y sus aplicaciones
                val jobResult = jobRepository.getJobById(jobId)
                val applicationsResult = jobRepository.getJobApplications(jobId)
                
                if (jobResult.isSuccess && applicationsResult.isSuccess) {
                    val updatedJob = jobResult.getOrNull()
                    val applications = applicationsResult.getOrNull() ?: emptyList()
                    
                    if (updatedJob != null) {
                        // Actualizar solo este trabajo en la lista
                        val currentJobs = _uiState.value.jobsWithApplications.toMutableList()
                        val index = currentJobs.indexOfFirst { it.job.id == jobId }
                        
                        val updatedJobWithApps = JobWithApplications(updatedJob, applications)
                        
                        if (index >= 0) {
                            // Reemplazar el trabajo existente
                            currentJobs[index] = updatedJobWithApps
                            Log.d("ClientDashboardViewModel", "✅ Trabajo $jobId actualizado en la lista (índice $index)")
                        } else {
                            // Si no existe, agregarlo (por si acaso)
                            currentJobs.add(updatedJobWithApps)
                            Log.d("ClientDashboardViewModel", "➕ Trabajo $jobId agregado a la lista")
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            jobsWithApplications = currentJobs,
                            isLoading = false
                        )
                        Log.d("ClientDashboardViewModel", "⚡⚡⚡ UI actualizada instantáneamente para trabajo $jobId")
                    }
                } else {
                    // Si falla, hacer refresh completo como fallback
                    Log.w("ClientDashboardViewModel", "⚠️ Error al actualizar trabajo $jobId, haciendo refresh completo...")
                    refreshJobs()
                }
            } catch (e: Exception) {
                Log.e("ClientDashboardViewModel", "❌ Error al actualizar trabajo $jobId: ${e.message}", e)
                // Fallback: refresh completo
                refreshJobs()
            }
        }
    }
    
    fun loadMyJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            jobRepository.getMyJobs()
                .onSuccess { jobs ->
                    // Cargar aplicaciones para cada trabajo
                    val jobsWithApps = jobs.map { job ->
                        val applicationsResult = jobRepository.getJobApplications(job.id)
                        val applications = applicationsResult.getOrElse { emptyList() }
                        JobWithApplications(job, applications)
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        jobsWithApplications = jobsWithApps
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Error al cargar trabajos"
                    )
                }
        }
    }
    
    fun refreshJobs() {
        loadMyJobs()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d("ClientDashboardViewModel", "🧹 Limpiando WebSocket...")
        webSocketClient.cleanup()
    }
}

