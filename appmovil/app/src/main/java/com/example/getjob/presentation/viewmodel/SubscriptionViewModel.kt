package com.example.getjob.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.getjob.data.models.responses.SubscriptionResponse
import com.example.getjob.data.models.responses.SubscriptionStatusResponse
import com.example.getjob.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSubscribing: Boolean = false,
    val status: SubscriptionStatusResponse? = null,
    val history: List<SubscriptionResponse> = emptyList(),
    val isSuccess: Boolean = false
)

class SubscriptionViewModel : ViewModel() {
    private val subscriptionRepository = SubscriptionRepository()
    
    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()
    
    init {
        loadStatus()
        loadHistory()
    }
    
    fun loadStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            subscriptionRepository.getStatus()
                .onSuccess { status ->
                    _uiState.value = _uiState.value.copy(
                        status = status,
                        isLoading = false
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Error al cargar estado de suscripción"
                    )
                }
        }
    }
    
    fun loadHistory() {
        viewModelScope.launch {
            subscriptionRepository.getHistory()
                .onSuccess { history ->
                    _uiState.value = _uiState.value.copy(history = history)
                }
                .onFailure { exception ->
                    // No mostrar error si falla el historial
                    if (_uiState.value.errorMessage == null) {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = exception.message ?: "Error al cargar historial"
                        )
                    }
                }
        }
    }
    
    fun subscribe(plan: String, paymentCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubscribing = true, errorMessage = null, isSuccess = false)
            
            subscriptionRepository.subscribe(plan, paymentCode)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSubscribing = false,
                        isSuccess = true
                    )
                    // Recargar estado después de suscribirse
                    loadStatus()
                    loadHistory()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isSubscribing = false,
                        errorMessage = exception.message ?: "Error al suscribirse"
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
    
    fun cancelSubscription() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            subscriptionRepository.cancel()
                .onSuccess {
                    // Actualizar estado inmediatamente para reflejar la cancelación
                    val currentStatus = _uiState.value.status
                    val updatedStatus = currentStatus?.copy(
                        is_plus_active = false,
                        plus_expires_at = null
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        status = updatedStatus
                    )
                    // Recargar estado desde el servidor para asegurar consistencia
                    loadStatus()
                    loadHistory()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Error al cancelar suscripción"
                    )
                }
        }
    }
}
