package com.example.getjob.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.getjob.data.models.responses.JobResponse
import com.example.getjob.data.repository.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PaymentAndReviewUiState(
    val isLoading: Boolean = false,
    val job: JobResponse? = null,
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false
)

class PaymentAndReviewViewModel : ViewModel() {
    private val jobRepository = JobRepository()

    private val _uiState = MutableStateFlow(PaymentAndReviewUiState())
    val uiState: StateFlow<PaymentAndReviewUiState> = _uiState.asStateFlow()

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

    fun rateJob(jobId: Int, rating: Int, comment: String? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            
            jobRepository.rateJob(jobId, rating, comment)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSubmitting = false)
                    onSuccess()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = exception.message ?: "Error al calificar trabajo"
                    )
                }
        }
    }
    
    fun finalizePaymentAndRate(
        jobId: Int,
        isAlreadyCompleted: Boolean,
        rating: Int,
        comment: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            try {
                if (!isAlreadyCompleted) {
                    jobRepository.completeJob(jobId)
                        .onSuccess { updatedJob ->
                            _uiState.value = _uiState.value.copy(job = updatedJob)
                        }
                        .onFailure { throw it }
                }
                
                jobRepository.rateJob(jobId, rating, comment)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(isSubmitting = false)
                        onSuccess()
                    }
                    .onFailure { throw it }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = e.message ?: "Error al finalizar pago y reseña"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

