package com.example.getjob.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.getjob.data.models.responses.JobResponse
import com.example.getjob.data.repository.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ClientRateWorkerUiState(
    val isLoading: Boolean = false,
    val job: JobResponse? = null,
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false
)

class ClientRateWorkerViewModel : ViewModel() {
    private val jobRepository = JobRepository()

    private val _uiState = MutableStateFlow(ClientRateWorkerUiState())
    val uiState: StateFlow<ClientRateWorkerUiState> = _uiState.asStateFlow()

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
                        errorMessage = exception.message ?: "Error al cargar el trabajo"
                    )
                }
        }
    }

    fun rateWorker(jobId: Int, rating: Int, comment: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            jobRepository.rateWorker(jobId, rating, comment)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        isSuccess = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = exception.message ?: "Error al calificar trabajador"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }
}

