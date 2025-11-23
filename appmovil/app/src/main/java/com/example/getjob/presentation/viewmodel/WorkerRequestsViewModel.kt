package com.example.getjob.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.getjob.data.models.responses.JobResponse
import com.example.getjob.data.models.responses.JobApplicationResponse
import com.example.getjob.data.repository.JobRepository
import com.example.getjob.data.repository.WorkerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import java.time.format.DateTimeParseException

// Modelo que combina Application con Job para evitar llamadas desde el Composable
data class ApplicationWithJob(
    val application: JobApplicationResponse,
    val job: JobResponse? = null,
    val isLoadingJob: Boolean = false,
    val jobError: String? = null
)

data class WorkerRequestsUiState(
    val isLoading: Boolean = false,
    val jobs: List<JobResponse> = emptyList(), // Trabajos aceptados
    val applications: List<ApplicationWithJob> = emptyList(), // Aplicaciones pendientes con sus jobs
    val errorMessage: String? = null, // Error general
    val isPlusActive: Boolean = false // Estado de Modo Plus activo
)

class WorkerRequestsViewModel : ViewModel() {
    private val jobRepository = JobRepository()
    private val workerRepository = WorkerRepository()
    
    private val _uiState = MutableStateFlow(WorkerRequestsUiState())
    val uiState: StateFlow<WorkerRequestsUiState> = _uiState.asStateFlow()
    
    // Timestamp de última carga para caché
    private var lastLoadTimestamp: Long? = null
    // Tiempo de validez del caché: 5 minutos
    private val CACHE_VALIDITY_MS = 5 * 60 * 1000L
    // Flag para saber si ya se intentó cargar al menos una vez
    private var hasAttemptedLoad = false
    
    init {
        // NO cargar automáticamente en el init
        // La carga se hará desde el Screen usando LaunchedEffect
        // Solo asegurar que isLoading sea false si hay datos
        val currentState = _uiState.value
        val hasData = currentState.applications.isNotEmpty() || currentState.jobs.isNotEmpty()
        
        if (hasData) {
            // Si hay datos, el ViewModel se está reutilizando (tiene caché)
            // Actualizar timestamp y asegurar que isLoading sea false
            lastLoadTimestamp = System.currentTimeMillis()
            if (currentState.isLoading) {
                _uiState.value = currentState.copy(isLoading = false)
            }
            hasAttemptedLoad = true
        }
    }
    
    /**
     * Inicializa la carga de datos. Solo carga si no hay datos y no se ha intentado cargar antes.
     * Este método debe ser llamado desde el Screen usando LaunchedEffect.
     */
    fun initializeIfNeeded() {
        if (hasAttemptedLoad) {
            return // Ya se intentó cargar antes
        }
        
        val currentState = _uiState.value
        val hasData = currentState.applications.isNotEmpty() || currentState.jobs.isNotEmpty()
        
        if (!hasData) {
            hasAttemptedLoad = true
            loadAllData()
        } else {
            hasAttemptedLoad = true
            lastLoadTimestamp = System.currentTimeMillis()
        }
    }
    
    /**
     * Carga todo junto: perfil (para Plus), trabajos aceptados y aplicaciones pendientes
     * @param forceRefresh Si es true, fuerza la recarga incluso si hay caché válido
     */
    fun loadAllData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val hasData = currentState.applications.isNotEmpty() || currentState.jobs.isNotEmpty()
            
            // Si hay datos y no se fuerza refresh, verificar caché
            if (!forceRefresh && hasData) {
                if (lastLoadTimestamp != null) {
                    val cacheValid = System.currentTimeMillis() - lastLoadTimestamp!! < CACHE_VALIDITY_MS
                    if (cacheValid) {
                        // Caché válido, no recargar
                        return@launch
                    }
                }
                // Si hay datos pero el caché expiró, recargar en segundo plano SIN mostrar loading
                try {
                    coroutineScope {
                        async { checkPlusStatus() }
                        async { loadMyJobsInternal() }
                        async { loadMyApplicationsInternal() }
                    }
                    lastLoadTimestamp = System.currentTimeMillis()
                } catch (e: Exception) {
                    _uiState.value = currentState.copy(
                        errorMessage = e.message ?: "Error al actualizar datos"
                    )
                }
                return@launch
            }
            
            // Solo mostrar loading si NO hay datos (primera carga o refresh forzado sin datos)
            if (!hasData) {
                _uiState.value = currentState.copy(
                    isLoading = true,
                    errorMessage = null
                )
            } else if (forceRefresh) {
                // Si hay datos pero se fuerza refresh, actualizar sin mostrar loading
                _uiState.value = currentState.copy(
                    isLoading = false,
                    errorMessage = null
                )
            }
            
            try {
                // Cargar todo en paralelo
                coroutineScope {
                    async { checkPlusStatus() }
                    async { loadMyJobsInternal() }
                    async { loadMyApplicationsInternal() }
                }
                // Actualizar timestamp de última carga
                lastLoadTimestamp = System.currentTimeMillis()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Error al cargar datos"
                )
            } finally {
                // Solo actualizar isLoading si estaba en true (primera carga)
                if (_uiState.value.isLoading) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }
    
    private suspend fun checkPlusStatus() {
        workerRepository.getMyProfile()
            .onSuccess { worker ->
                _uiState.value = _uiState.value.copy(
                    isPlusActive = worker.is_plus_active
                )
            }
            .onFailure {
                // Si falla, mantener el valor por defecto (false)
            }
    }
    
    private suspend fun loadMyJobsInternal() {
        jobRepository.getMyJobs()
            .onSuccess { jobs ->
                // Filtrar solo trabajos activos (excluir completados y cancelados)
                val activeJobs = jobs.filter { job ->
                    val status = job.status?.lowercase() ?: ""
                    status in listOf("accepted", "in_route", "on_site", "in_progress")
                }
                
                // Ordenar por estado (prioridad) y luego por fecha
                val statusOrder = mapOf(
                    "in_progress" to 1,
                    "on_site" to 2,
                    "in_route" to 3,
                    "accepted" to 4
                )
                
                val sortedJobs = activeJobs.sortedWith(
                    compareBy<JobResponse> { job ->
                        val status = job.status?.lowercase() ?: ""
                        statusOrder[status] ?: 999
                    }.thenByDescending { job ->
                        try {
                            Instant.parse(job.created_at).toEpochMilli()
                        } catch (e: DateTimeParseException) {
                            job.created_at.hashCode().toLong()
                        }
                    }
                )
                
                _uiState.value = _uiState.value.copy(jobs = sortedJobs)
            }
            .onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = exception.message ?: "Error al cargar solicitudes"
                )
            }
    }
    
    private suspend fun loadMyApplicationsInternal() {
        jobRepository.getMyApplications()
            .onSuccess { applications ->
                // Filtrar solo aplicaciones no aceptadas (pendientes)
                val pendingApplications = applications.filter { !it.is_accepted }
                
                // Inicializar con ApplicationWithJob (sin job cargado aún)
                val applicationsWithJobs = pendingApplications.map { application ->
                    ApplicationWithJob(application = application, isLoadingJob = true)
                }
                
                _uiState.value = _uiState.value.copy(applications = applicationsWithJobs)
                
                // Cargar los jobs en paralelo para cada aplicación
                loadJobsForApplications(pendingApplications)
            }
            .onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = exception.message ?: "Error al cargar aplicaciones"
                )
            }
    }
    
    private fun loadJobsForApplications(applications: List<JobApplicationResponse>) {
        viewModelScope.launch {
            // Cargar todos los jobs en paralelo
            applications.forEach { application ->
                if (application.job_id > 0) {
                    launch {
                        jobRepository.getJobById(application.job_id)
                            .onSuccess { job ->
                                // Actualizar el estado con el job cargado
                                val currentApplications = _uiState.value.applications.toMutableList()
                                val index = currentApplications.indexOfFirst { it.application.id == application.id }
                                if (index >= 0) {
                                    currentApplications[index] = ApplicationWithJob(
                                        application = application,
                                        job = job,
                                        isLoadingJob = false
                                    )
                                    _uiState.value = _uiState.value.copy(applications = currentApplications)
                                }
                            }
                            .onFailure { exception ->
                                // Actualizar con error
                                val currentApplications = _uiState.value.applications.toMutableList()
                                val index = currentApplications.indexOfFirst { it.application.id == application.id }
                                if (index >= 0) {
                                    currentApplications[index] = ApplicationWithJob(
                                        application = application,
                                        job = null,
                                        isLoadingJob = false,
                                        jobError = exception.message ?: "Error al cargar trabajo"
                                    )
                                    _uiState.value = _uiState.value.copy(applications = currentApplications)
                                }
                            }
                    }
                }
            }
        }
    }
    
    fun retryLoadJob(applicationId: Int) {
        viewModelScope.launch {
            val applicationWithJob = _uiState.value.applications.find { it.application.id == applicationId }
            if (applicationWithJob != null && applicationWithJob.application.job_id > 0) {
                // Actualizar estado a loading
                val currentApplications = _uiState.value.applications.toMutableList()
                val index = currentApplications.indexOfFirst { it.application.id == applicationId }
                if (index >= 0) {
                    currentApplications[index] = ApplicationWithJob(
                        application = applicationWithJob.application,
                        job = null,
                        isLoadingJob = true,
                        jobError = null
                    )
                    _uiState.value = _uiState.value.copy(applications = currentApplications)
                    
                    // Intentar cargar de nuevo
                    jobRepository.getJobById(applicationWithJob.application.job_id)
                        .onSuccess { job ->
                            currentApplications[index] = ApplicationWithJob(
                                application = applicationWithJob.application,
                                job = job,
                                isLoadingJob = false
                            )
                            _uiState.value = _uiState.value.copy(applications = currentApplications)
                        }
                        .onFailure { exception ->
                            currentApplications[index] = ApplicationWithJob(
                                application = applicationWithJob.application,
                                job = null,
                                isLoadingJob = false,
                                jobError = exception.message ?: "Error al cargar trabajo"
                            )
                            _uiState.value = _uiState.value.copy(applications = currentApplications)
                        }
                }
            }
        }
    }
    
    fun refresh() {
        // Forzar refresh cuando el usuario lo solicita manualmente
        loadAllData(forceRefresh = true)
    }
    
    fun refreshApplications() {
        viewModelScope.launch {
            loadMyApplicationsInternal()
        }
    }
    
    fun refreshJobs() {
        viewModelScope.launch {
            loadMyJobsInternal()
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

