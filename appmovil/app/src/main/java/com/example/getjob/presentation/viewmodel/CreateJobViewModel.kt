package com.example.getjob.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.getjob.data.models.requests.CreateJobRequest
import com.example.getjob.data.models.responses.JobResponse
import com.example.getjob.data.repository.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class CreateJobUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val createdJob: JobResponse? = null
)

class CreateJobViewModel : ViewModel() {
    private val jobRepository = JobRepository()
    
    private val _uiState = MutableStateFlow(CreateJobUiState())
    val uiState: StateFlow<CreateJobUiState> = _uiState.asStateFlow()
    
    fun createJob(
        title: String,
        description: String?,
        serviceType: String,
        paymentMethod: String,
        baseFee: BigDecimal,
        address: String,
        latitude: BigDecimal? = null,
        longitude: BigDecimal? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val request = CreateJobRequest(
                title = title,
                description = description,
                service_type = serviceType,
                payment_method = paymentMethod,
                base_fee = baseFee,
                address = address,
                latitude = latitude,
                longitude = longitude
            )
            
            jobRepository.createJob(request)
                .onSuccess { job ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        createdJob = job
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Error al crear trabajo"
                    )
                }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false, createdJob = null)
    }
}

