package com.example.getjob.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.getjob.data.models.responses.JobResponse
import com.example.getjob.data.repository.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class JobDetailUiState(
    val isLoading: Boolean = false,
    val job: JobResponse? = null,
    val errorMessage: String? = null,
    val isStarting: Boolean = false,
    val isCancelling: Boolean = false
)

class JobDetailViewModel : ViewModel() {
    private val jobRepository = JobRepository()
    
    private val _uiState = MutableStateFlow(JobDetailUiState())
    val uiState: StateFlow<JobDetailUiState> = _uiState.asStateFlow()
    
    fun loadJob(jobId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            jobRepository.getJobById(jobId)
                .onSuccess { job ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        job = job
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Error al cargar trabajo"
                    )
                }
        }
    }
    
    fun startRoute(jobId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStarting = true, errorMessage = null)
            
            jobRepository.startRoute(jobId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isStarting = false)
                    onSuccess()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isStarting = false,
                        errorMessage = exception.message ?: "Error al iniciar ruta"
                    )
                }
        }
    }
    
    fun confirmArrival(jobId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStarting = true, errorMessage = null)
            
            jobRepository.confirmArrival(jobId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isStarting = false)
                    loadJob(jobId) // Recargar para actualizar estado
                    onSuccess()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isStarting = false,
                        errorMessage = exception.message ?: "Error al confirmar llegada"
                    )
                }
        }
    }
    
    fun startService(jobId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStarting = true, errorMessage = null)
            
            jobRepository.startService(jobId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isStarting = false)
                    loadJob(jobId) // Recargar para actualizar estado
                    onSuccess()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isStarting = false,
                        errorMessage = exception.message ?: "Error al iniciar servicio"
                    )
                }
        }
    }
    
    fun addExtra(jobId: Int, extraAmount: java.math.BigDecimal, description: String? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStarting = true, errorMessage = null)
            
            jobRepository.addExtra(jobId, extraAmount, description)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isStarting = false)
                    loadJob(jobId) // Recargar para actualizar estado con el nuevo extra
                    onSuccess()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isStarting = false,
                        errorMessage = exception.message ?: "Error al agregar extra"
                    )
                }
        }
    }
    
    fun completeJob(jobId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStarting = true, errorMessage = null)
            
            jobRepository.completeJob(jobId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isStarting = false)
                    loadJob(jobId) // Recargar para actualizar estado
                    onSuccess()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isStarting = false,
                        errorMessage = exception.message ?: "Error al completar trabajo"
                    )
                }
        }
    }
    
    fun cancelJob(jobId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCancelling = true, errorMessage = null)
            
            jobRepository.cancelJob(jobId)
                .onSuccess {
                    // Recargar el trabajo para actualizar el estado
                    loadJob(jobId)
                    onSuccess()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isCancelling = false,
                        errorMessage = exception.message ?: "Error al cancelar trabajo"
                    )
                }
        }
    }
    
    // Función eliminada: setJobToPending() no tiene sentido en la lógica de negocio
    // Un trabajo cancelado no puede volver a PENDING
    // Si se necesita liberar un trabajo, debe cancelarse y el cliente crear uno nuevo
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

