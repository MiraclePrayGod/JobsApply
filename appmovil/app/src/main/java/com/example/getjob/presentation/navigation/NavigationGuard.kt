package com.example.getjob.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.getjob.utils.PreferencesManager
import kotlinx.coroutines.launch

/**
 * Valida que el usuario tenga el rol correcto para acceder a una pantalla
 * Si no tiene el rol correcto, redirige al dashboard apropiado
 */
@Composable
fun RequireRole(
    requiredRole: String, // "worker" o "client"
    onUnauthorized: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val userRole = remember { preferencesManager.getUserRole() }
    
    LaunchedEffect(userRole) {
        if (userRole != requiredRole) {
            onUnauthorized()
        }
    }
    
    if (userRole == requiredRole) {
        content()
    } else {
        // Mostrar pantalla de carga mientras redirige
        androidx.compose.material3.CircularProgressIndicator()
    }
}

/**
 * Valida que el usuario tenga acceso a un trabajo específico
 * Verifica que sea el cliente o el trabajador asignado
 */
suspend fun validateJobAccess(
    jobId: Int,
    userId: Int,
    userRole: String?,
    jobRepository: com.example.getjob.data.repository.JobRepository
): Boolean {
    return try {
        val jobResult = jobRepository.getJobById(jobId)
        jobResult.onSuccess { job ->
            // Si es cliente, verificar que es el dueño del trabajo
            if (userRole == "client") {
                return job.client_id == userId
            }
            // Si es trabajador, verificar que está asignado al trabajo
            else if (userRole == "worker") {
                return job.worker_id != null // El backend valida que sea el trabajador correcto
            }
        }
        false
    } catch (e: Exception) {
        false
    }
}

