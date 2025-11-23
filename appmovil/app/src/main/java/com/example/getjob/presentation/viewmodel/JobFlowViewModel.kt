    package com.example.getjob.presentation.viewmodel

    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.example.getjob.data.models.responses.JobResponse
    import com.example.getjob.data.repository.JobRepository
    import com.example.getjob.data.repository.WorkerRepository
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.asStateFlow
    import kotlinx.coroutines.launch

    enum class JobStage {
        ACCEPTED,
        ON_ROUTE,
        ARRIVED,
        IN_SERVICE,
        COMPLETED,
        REVIEWED
    }

    fun mapStatusToStage(status: String): JobStage = when (status.lowercase()) {
        "accepted" -> JobStage.ACCEPTED
        "in_route" -> JobStage.ON_ROUTE
        "on_site", "arrived" -> JobStage.ARRIVED
        "in_progress" -> JobStage.IN_SERVICE
        "completed" -> JobStage.COMPLETED
        "reviewed" -> JobStage.REVIEWED
        else -> JobStage.ACCEPTED
    }

    data class JobFlowUiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val job: JobResponse? = null,
        val stage: JobStage = JobStage.ACCEPTED,
        val elapsedTime: String = "00:00:00",
        val isPlusActive: Boolean = false,
        val isStarting: Boolean = false,
        val isSubmitting: Boolean = false
    )

    class JobFlowViewModel : ViewModel() {
        private val jobRepository = JobRepository()
        private val workerRepository = WorkerRepository()
        
        private val _uiState = MutableStateFlow(JobFlowUiState())
        val uiState: StateFlow<JobFlowUiState> = _uiState.asStateFlow()
        
        fun loadJob(jobId: Int, forceRefresh: Boolean = false) {
            val currentJob = _uiState.value.job
            if (!forceRefresh && currentJob?.id == jobId) {
                // Ya tenemos este job cargado, no vuelvas a llamar a la API
                return
            }
            
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                jobRepository.getJobById(jobId)
                    .onSuccess { job ->
                        val stage = mapStatusToStage(job.status)
                        val plusActive = checkPlusStatus()
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            job = job,
                            stage = stage,
                            isPlusActive = plusActive
                        )
                    }
                    .onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Error al cargar el trabajo"
                        )
                    }
            }
        }
        
        private suspend fun checkPlusStatus(): Boolean {
            return try {
                workerRepository.getMyProfile()
                    .getOrNull()
                    ?.let { worker ->
                        worker.is_plus_active
                    } ?: false
            } catch (e: Exception) {
                false
            }
        }
        
        fun startRoute(jobId: Int, onSuccess: () -> Unit) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isStarting = true, errorMessage = null)
                
                jobRepository.startRoute(jobId)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isStarting = false,
                            stage = JobStage.ON_ROUTE
                        )
                        loadJob(jobId, forceRefresh = true) // Recargar para obtener estado actualizado
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
                        _uiState.value = _uiState.value.copy(
                            isStarting = false,
                            stage = JobStage.ARRIVED
                        )
                        loadJob(jobId)
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
                        _uiState.value = _uiState.value.copy(
                            isStarting = false,
                            stage = JobStage.IN_SERVICE
                        )
                        loadJob(jobId)
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
                        loadJob(jobId)
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
        
        fun completeService(jobId: Int, onSuccess: () -> Unit) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isStarting = true, errorMessage = null)
                
                jobRepository.completeJob(jobId)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isStarting = false,
                            stage = JobStage.COMPLETED
                        )
                        loadJob(jobId)
                        onSuccess()
                    }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isStarting = false,
                            errorMessage = exception.message ?: "Error al completar servicio"
                        )
                    }
            }
        }
        
        fun submitRating(jobId: Int, rating: Int, comment: String?, onSuccess: () -> Unit) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
                
                jobRepository.rateJob(jobId, rating, comment)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            stage = JobStage.REVIEWED
                        )
                        onSuccess()
                    }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            errorMessage = exception.message ?: "Error al enviar calificación"
                        )
                    }
            }
        }
        
        fun cancelJob(jobId: Int, onSuccess: () -> Unit) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isStarting = true, errorMessage = null)
                
                jobRepository.cancelJob(jobId)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(isStarting = false)
                        onSuccess()
                    }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isStarting = false,
                            errorMessage = exception.message ?: "Error al cancelar trabajo"
                        )
                    }
            }
        }
        
        fun clearError() {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }

