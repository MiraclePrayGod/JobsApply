package com.example.getjob.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.getjob.data.models.responses.UserResponse
import com.example.getjob.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ClientProfileUiState(
    val isLoading: Boolean = false,
    val user: UserResponse? = null,
    val errorMessage: String? = null,
    val isUpdating: Boolean = false
)

class ClientProfileViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    
    private val _uiState = MutableStateFlow(ClientProfileUiState())
    val uiState: StateFlow<ClientProfileUiState> = _uiState.asStateFlow()
    
    init {
        loadProfile()
    }
    
    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            authRepository.getCurrentUser()
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Error al cargar el perfil"
                    )
                }
        }
    }
    
    fun refreshProfile() {
        loadProfile()
    }
    
    fun updateProfile(
        email: String? = null,
        password: String? = null,
        fullName: String? = null,
        phone: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true, errorMessage = null)
            
            authRepository.updateProfile(
                email = email,
                password = password,
                fullName = fullName,
                phone = phone
            )
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        user = user
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        errorMessage = exception.message ?: "Error al actualizar perfil"
                    )
                }
        }
    }
}

