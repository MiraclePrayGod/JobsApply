package com.example.getjob.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.getjob.data.models.responses.JobResponse
import com.example.getjob.data.repository.JobRepository
import com.example.getjob.data.repository.WorkerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.math.BigDecimal

data class DashboardUiState(
    val isLoading: Boolean = false,
    val availableJobs: List<JobResponse> = emptyList(),
    val myJobs: List<JobResponse> = emptyList(),
    val selectedServiceFilter: String? = null,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isRefreshing: Boolean = false,
    val isProfileComplete: Boolean = true, // Por defecto true, se verifica al cargar
    val isCheckingProfile: Boolean = false,
    val isWorkerVerified: Boolean = false, // Estado de verificación del trabajador
    val isWorkerAvailable: Boolean = false, // Estado de disponibilidad del trabajador
    val isPlusActive: Boolean = false, // Estado de Modo Plus activo
    val plusExpiresAt: String? = null, // Fecha de expiración de Modo Plus
    val isUpdatingAvailability: Boolean = false, // Estado de actualización de disponibilidad
    val isApplyingToJob: Boolean = false, // Estado de aplicación a trabajo
    val applyingJobId: Int? = null, // ID del trabajo al que se está aplicando
    val estimatedEarnings: Double = 0.0, // Ganancias estimadas de la semana
    val completedJobsCount: Int = 0, // Cantidad de trabajos completados
    val isLoadingEarnings: Boolean = false // Estado de carga de ganancias
)

class DashboardViewModel : ViewModel() {
    private val jobRepository = JobRepository()
    private val workerRepository = WorkerRepository()
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    // Flujo de búsqueda con debounce
    private val _searchQuery = MutableStateFlow("")
    
    init {
        checkProfileStatus()
        loadJobs()
        loadEarnings()
        observeSearch()
    }
    
    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(400) // espera 400 ms sin cambios
                .distinctUntilChanged() // ignora mismas búsquedas
                .collectLatest { query ->
                    // Actualizar el query en el estado
                    _uiState.value = _uiState.value.copy(searchQuery = query)
                    // Llamar a loadJobs con el query actualizado
                    loadJobs()
                }
        }
    }
    
    fun checkProfileStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingProfile = true)
            workerRepository.getMyProfile()
                .onSuccess { worker ->
                    // Si existe el perfil, está completo
                    _uiState.value = _uiState.value.copy(
                        isProfileComplete = true,
                        isCheckingProfile = false,
                        isWorkerVerified = worker.is_verified,
                        isWorkerAvailable = worker.is_available,
                        isPlusActive = worker.is_plus_active, // Usar el valor real del backend
                        plusExpiresAt = worker.plus_expires_at
                    )
                }
                .onFailure {
                    // Si no existe (404) o hay error, el perfil está incompleto
                    _uiState.value = _uiState.value.copy(
                        isProfileComplete = false,
                        isCheckingProfile = false,
                        isWorkerVerified = false,
                        isWorkerAvailable = false,
                        isPlusActive = false,
                        plusExpiresAt = null
                    )
                }
        }
    }
    
    fun loadJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val searchQuery = if (_uiState.value.searchQuery.isBlank()) null else _uiState.value.searchQuery
            jobRepository.getAvailableJobs(_uiState.value.selectedServiceFilter, searchQuery)
                .onSuccess { jobs ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        availableJobs = jobs
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            loadJobs()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }
    
    fun filterByService(serviceType: String?) {
        _uiState.value = _uiState.value.copy(selectedServiceFilter = serviceType)
        loadJobs()
    }
    
    fun onSearchQueryChange(query: String) {
        // Actualizar el estado inmediatamente para que la UI se actualice
        _uiState.value = _uiState.value.copy(searchQuery = query)
        // Actualizar el flujo de búsqueda (el debounce se encargará de llamar a loadJobs)
        _searchQuery.value = query
    }
    
    // Mantener searchJobs por compatibilidad, pero ahora usa el flujo con debounce
    @Deprecated("Usar onSearchQueryChange en su lugar", ReplaceWith("onSearchQueryChange(query)"))
    fun searchJobs(query: String) {
        onSearchQueryChange(query)
    }
    
    fun applyToJob(
        jobId: Int, 
        onSuccess: () -> Unit, 
        onProfileIncomplete: () -> Unit, 
        onAvailabilityRequired: () -> Unit,
        onPlusRequired: () -> Unit
    ) {
        viewModelScope.launch {
            // Verificar primero si el perfil está completo
            if (!_uiState.value.isProfileComplete) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = " completa tu perfil profesional para poder aplicar a trabajos. Completa tu información en la sección de Perfil."
                )
                onProfileIncomplete()
                return@launch
            }
            
            // Verificar que el trabajador esté disponible
            if (!_uiState.value.isWorkerAvailable) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Debes activar tu disponibilidad para aplicar a trabajos"
                )
                onAvailabilityRequired()
                return@launch
            }
            
            // Mostrar estado de carga inmediatamente
            _uiState.value = _uiState.value.copy(
                errorMessage = null, 
                successMessage = null,
                isApplyingToJob = true,
                applyingJobId = jobId
            )
            
            val result = jobRepository.applyToJob(jobId)
            result.onSuccess { jobResponse ->
                _uiState.value = _uiState.value.copy(
                    isApplyingToJob = false,
                    applyingJobId = null,
                    successMessage = null // Asegurar que no hay mensaje de éxito
                )
                // Navegar inmediatamente a Solicitudes (sin mensaje de éxito)
                android.util.Log.d("DashboardViewModel", "Aplicación exitosa, navegando a Solicitudes")
                onSuccess()
            }
            result.onFailure { exception ->
                val errorMessage = exception.message ?: "Error al aplicar al trabajo"
                val alreadyApplied = errorMessage.contains("ya has aplicado", ignoreCase = true) ||
                        errorMessage.contains("ya aplicaste", ignoreCase = true) ||
                        errorMessage.contains("already applied", ignoreCase = true)
                
                val plusRequired = errorMessage.contains("Modo Plus", ignoreCase = true) ||
                        errorMessage.contains("plus activo", ignoreCase = true) ||
                        errorMessage.contains("plan", ignoreCase = true) ||
                        exception.message?.contains("403", ignoreCase = true) == true

                if (alreadyApplied) {
                    // Tratar como éxito: ya estaba en solicitudes, solo navegar
                    _uiState.value = _uiState.value.copy(
                        errorMessage = null,
                        isApplyingToJob = false,
                        applyingJobId = null
                    )
                    android.util.Log.d("DashboardViewModel", "Aplicación ya existente, navegando igualmente a Solicitudes")
                    onSuccess()
                } else if (plusRequired) {
                    // Plus requerido
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Necesitas Modo Plus activo para aplicar a trabajos",
                        isApplyingToJob = false,
                        applyingJobId = null
                    )
                    onPlusRequired()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = errorMessage,
                        isApplyingToJob = false,
                        applyingJobId = null
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    fun refreshProfileStatus() {
        checkProfileStatus()
    }
    
    fun toggleAvailability(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val newAvailability = !_uiState.value.isWorkerAvailable
            _uiState.value = _uiState.value.copy(
                errorMessage = null, 
                successMessage = null,
                isUpdatingAvailability = true
            )
            
            workerRepository.updateProfile(isAvailable = newAvailability)
                .onSuccess { worker ->
                    _uiState.value = _uiState.value.copy(
                        isWorkerAvailable = worker.is_available,
                        isUpdatingAvailability = false,
                        successMessage = if (worker.is_available) 
                            "Disponibilidad activada" 
                        else 
                            "Disponibilidad desactivada"
                    )
                    // Los trabajos siempre se muestran (estilo TikTok) - sin importar disponibilidad
                    // La disponibilidad solo se verifica al aplicar a un trabajo
                    onSuccess()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isUpdatingAvailability = false,
                        errorMessage = exception.message ?: "Error al actualizar disponibilidad"
                    )
                }
        }
    }
    
    fun loadEarnings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingEarnings = true)
            
            jobRepository.getMyJobs()
                .onSuccess { allJobs ->
                    // Calcular ganancias de la semana actual desde trabajos completados
                    val calendar = Calendar.getInstance()
                    val startOfWeek = calendar.clone() as Calendar
                    startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    startOfWeek.set(Calendar.HOUR_OF_DAY, 0)
                    startOfWeek.set(Calendar.MINUTE, 0)
                    startOfWeek.set(Calendar.SECOND, 0)
                    startOfWeek.set(Calendar.MILLISECOND, 0)
                    
                    // Filtrar trabajos completados de esta semana
                    val completedJobsThisWeek = allJobs.filter { job ->
                        val isCompleted = job.status.lowercase() == "completed"
                        if (!isCompleted) return@filter false
                        
                        // Verificar si fue completado esta semana
                        try {
                            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                            val completedDate = job.completed_at?.let { dateFormat.parse(it) }
                            completedDate != null && completedDate.time >= startOfWeek.timeInMillis
                        } catch (e: Exception) {
                            // Si no hay fecha de completado, usar created_at como fallback
                            try {
                                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                                val createdDate = dateFormat.parse(job.created_at)
                                createdDate != null && createdDate.time >= startOfWeek.timeInMillis
                            } catch (e2: Exception) {
                                false
                            }
                        }
                    }
                    
                    // Sumar total_amount de trabajos completados esta semana
                    val weekEarnings = completedJobsThisWeek
                        .sumOf { job ->
                            job.total_amount?.toDouble() ?: 0.0
                        }
                    
                    // Contar todos los trabajos completados (no solo de esta semana)
                    val completedCount = allJobs.count { 
                        it.status.lowercase() == "completed" 
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        estimatedEarnings = weekEarnings,
                        completedJobsCount = completedCount,
                        isLoadingEarnings = false
                    )
                }
                .onFailure { exception ->
                    // Si falla, mantener valores por defecto
                    _uiState.value = _uiState.value.copy(
                        isLoadingEarnings = false,
                        estimatedEarnings = 0.0,
                        completedJobsCount = 0
                    )
                    // No mostrar error para no molestar al usuario
                    android.util.Log.w("DashboardViewModel", "Error al cargar ganancias: ${exception.message}")
                }
        }
    }
    
    fun refreshEarnings() {
        loadEarnings()
    }
}

