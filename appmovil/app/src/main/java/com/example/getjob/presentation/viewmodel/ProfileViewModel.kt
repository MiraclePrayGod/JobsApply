package com.example.getjob.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.getjob.data.models.responses.WorkerResponse
import com.example.getjob.data.repository.WorkerRepository
import com.example.getjob.utils.ErrorParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estados de la pantalla de perfil usando sealed class para type-safety
 */
sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val worker: WorkerResponse) : ProfileUiState()
    data class Error(val error: ProfileError) : ProfileUiState()
}

/**
 * Errores tipados para mejor manejo y mensajes de usuario
 */
sealed class ProfileError {
    object Network : ProfileError() {
        val message = "Error de conexión. Por favor verifica tu conexión a internet e intenta nuevamente."
    }
    object Server : ProfileError() {
        val message = "Error del servidor. Por favor intenta más tarde."
    }
    data class Validation(val message: String) : ProfileError()
    data class Unknown(val message: String) : ProfileError()
}

/**
 * Estado específico para acciones secundarias (verificación, actualización)
 */
data class ProfileActionState(
    val isSubmittingVerification: Boolean = false,
    val verificationSuccess: Boolean = false,
    val isUpdatingProfile: Boolean = false
)

/**
 * Request object para actualizar perfil (evita múltiples parámetros opcionales)
 */
data class UpdateProfileRequest(
    val fullName: String? = null,
    val phone: String? = null,
    val services: List<String>? = null,
    val description: String? = null,
    val district: String? = null,
    val isAvailable: Boolean? = null,
    val yapeNumber: String? = null,
    val profileImageUrl: String? = null
)

/**
 * ViewModel refactorizado con:
 * - DI por constructor (testeable)
 * - Sealed class UI State (type-safe)
 * - Mejor manejo de errores (tipados)
 * - UpdateProfileRequest (evita parámetros opcionales)
 */
class ProfileViewModel(
    private val workerRepository: WorkerRepository = WorkerRepository()
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    private val _actionState = MutableStateFlow(ProfileActionState())
    val actionState: StateFlow<ProfileActionState> = _actionState.asStateFlow()
    
    init {
        loadProfile()
    }
    
    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            workerRepository.getMyProfile()
                .onSuccess { worker ->
                    _uiState.value = ProfileUiState.Success(worker)
                }
                .onFailure { exception ->
                    val error = mapExceptionToError(exception)
                    _uiState.value = ProfileUiState.Error(error)
                }
        }
    }
    
    fun refreshProfile() {
        loadProfile()
    }
    
    /**
     * Procesa y envía imagen de verificación
     * @param imageDataUri Data URI de la imagen (formato: "data:image/jpeg;base64,...")
     */
    fun submitVerification(imageDataUri: String) {
        viewModelScope.launch {
            _actionState.value = _actionState.value.copy(
                isSubmittingVerification = true,
                verificationSuccess = false
            )
            
            workerRepository.submitVerification(imageDataUri)
                .onSuccess { worker ->
                    _actionState.value = _actionState.value.copy(
                        isSubmittingVerification = false,
                        verificationSuccess = true
                    )
                    // Actualizar el estado principal con el worker actualizado
                    _uiState.value = ProfileUiState.Success(worker)
                }
                .onFailure { exception ->
                    val error = mapExceptionToError(exception)
                    _actionState.value = _actionState.value.copy(
                        isSubmittingVerification = false
                    )
                    _uiState.value = ProfileUiState.Error(error)
                }
        }
    }
    
    fun clearVerificationSuccess() {
        _actionState.value = _actionState.value.copy(verificationSuccess = false)
    }
    
    /**
     * Actualiza el perfil usando un request object
     */
    fun updateProfile(request: UpdateProfileRequest) {
        viewModelScope.launch {
            _actionState.value = _actionState.value.copy(isUpdatingProfile = true)
            
            workerRepository.updateProfile(
                fullName = request.fullName,
                phone = request.phone,
                services = request.services,
                description = request.description,
                district = request.district,
                isAvailable = request.isAvailable,
                yapeNumber = request.yapeNumber,
                profileImageUrl = request.profileImageUrl
            )
                .onSuccess { worker ->
                    _actionState.value = _actionState.value.copy(isUpdatingProfile = false)
                    _uiState.value = ProfileUiState.Success(worker)
                }
                .onFailure { exception ->
                    val error = mapExceptionToError(exception)
                    _actionState.value = _actionState.value.copy(isUpdatingProfile = false)
                    _uiState.value = ProfileUiState.Error(error)
                }
        }
    }
    
    /**
     * Helper para actualizar disponibilidad (más específico que updateProfile)
     */
    fun updateAvailability(isAvailable: Boolean) {
        updateProfile(UpdateProfileRequest(isAvailable = isAvailable))
    }
    
    /**
     * Helper para actualizar foto de perfil (más específico que updateProfile)
     */
    fun updateProfileImage(imageUrl: String) {
        updateProfile(UpdateProfileRequest(profileImageUrl = imageUrl))
    }
    
    /**
     * Mapea excepciones a errores tipados con mensajes de usuario
     */
    private fun mapExceptionToError(exception: Throwable): ProfileError {
        val errorMessage = ErrorParser.parseError(exception)
        
        return when {
            exception is java.net.SocketTimeoutException ||
            exception is java.net.ConnectException ||
            exception is java.net.UnknownHostException ||
            exception is java.io.IOException -> {
                ProfileError.Network
            }
            exception.message?.contains("4", ignoreCase = true) == true ||
            exception.message?.contains("5", ignoreCase = true) == true -> {
                ProfileError.Server
            }
            errorMessage.contains("validación", ignoreCase = true) ||
            errorMessage.contains("validation", ignoreCase = true) -> {
                ProfileError.Validation(errorMessage)
            }
            else -> {
                ProfileError.Unknown(errorMessage)
            }
        }
    }
}
