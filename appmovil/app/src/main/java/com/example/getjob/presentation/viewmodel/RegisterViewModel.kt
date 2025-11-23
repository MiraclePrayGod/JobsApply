package com.example.getjob.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.getjob.data.models.requests.WorkerRegisterRequest
import com.example.getjob.data.repository.AuthRepository
import com.example.getjob.data.repository.WorkerRepository
import com.example.getjob.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegisterUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val userId: Int? = null,
    val userRole: String? = null,
    val isWorkerProfileComplete: Boolean = false
)

class RegisterViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository()
    private val workerRepository = WorkerRepository()
    private val preferencesManager = PreferencesManager(application)
    
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()
    
    fun register(
        email: String, 
        password: String, 
        role: String = "worker",
        fullName: String? = null,
        phone: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            authRepository.register(email, password, role, fullName, phone)
                .onSuccess { userResponse ->
                    // Después del registro exitoso, hacer login automático para obtener el token
                    authRepository.login(email, password)
                        .onSuccess { authResponse ->
                            // Guardar datos en SharedPreferences con token
                            preferencesManager.saveAuthData(
                                token = authResponse.access_token,
                                userId = authResponse.user.id,
                                email = authResponse.user.email,
                                role = authResponse.user.role
                            )
                            
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isSuccess = true,
                                userId = userResponse.id,
                                userRole = authResponse.user.role
                            )
                        }
                        .onFailure { loginException ->
                            // Si el login falla después del registro, aún marcamos el registro como exitoso
                            // pero el usuario tendrá que hacer login manualmente
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isSuccess = true,
                                userId = userResponse.id,
                                userRole = userResponse.role,
                                errorMessage = "Registro exitoso, pero error al iniciar sesión automáticamente"
                            )
                        }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Error al registrar"
                    )
                }
        }
    }
    
    fun updateWorkerProfile(
        fullName: String?,
        phone: String?,
        services: List<String>?,
        description: String?,
        district: String?,
        isAvailable: Boolean?,
        yapeNumber: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            workerRepository.updateProfile(
                fullName = fullName,
                phone = phone,
                services = services,
                description = description,
                district = district,
                isAvailable = isAvailable,
                yapeNumber = yapeNumber
            )
                .onSuccess { workerResponse ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isWorkerProfileComplete = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Error al actualizar el perfil"
                    )
                }
        }
    }
    
    fun registerWorkerProfile(
        userId: Int,
        fullName: String,
        phone: String?,
        services: List<String>?,
        description: String?,
        district: String?,
        isAvailable: Boolean,
        yapeNumber: String?
    ) {
        viewModelScope.launch {
            // Validar campos requeridos antes de enviar
            if (fullName.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "El nombre completo es requerido"
                )
                return@launch
            }
            
            if (services.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Debes seleccionar al menos un servicio"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val request = WorkerRegisterRequest(
                user_id = userId,
                full_name = fullName,
                phone = phone,
                services = services,
                description = description,
                district = district,
                is_available = isAvailable,
                yape_number = yapeNumber
            )
            
            workerRepository.registerWorkerProfile(request)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isWorkerProfileComplete = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Error al completar perfil de trabajador"
                    )
                }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false, userRole = null, isWorkerProfileComplete = false)
    }
}

