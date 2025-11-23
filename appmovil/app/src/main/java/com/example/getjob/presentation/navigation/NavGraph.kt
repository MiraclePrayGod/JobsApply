package com.example.getjob.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.getjob.presentation.components.WorkerBottomNavigationBar
import com.example.getjob.data.repository.WorkerRepository
import com.example.getjob.presentation.screens.login.LoginScreen
import com.example.getjob.presentation.screens.register.RegisterWorkerScreen
import com.example.getjob.presentation.screens.register.RegisterClientScreen
import com.example.getjob.presentation.screens.jobdetail.JobDetailScreen as SolicitudDetailScreen
import com.example.getjob.presentation.screens.jobflow.JobFlowScreen
// Pantallas antiguas (deprecated - usar JobFlowScreen en su lugar)
// import com.example.getjob.presentation.screens.onroute.OnRouteScreen
// import com.example.getjob.presentation.screens.onsite.OnSiteScreen
// import com.example.getjob.presentation.screens.service.ServiceInProgressScreen
// import com.example.getjob.presentation.screens.payment.PaymentAndReviewScreen
import com.example.getjob.presentation.screens.commissions.PendingCommissionsScreen
import com.example.getjob.presentation.screens.client.ClientDashboardScreen
import com.example.getjob.presentation.screens.client.CreateJobScreen
import com.example.getjob.presentation.screens.client.ClientRateWorkerScreen
import com.example.getjob.presentation.screens.requests.WorkerRequestsScreen
import com.example.getjob.presentation.screens.chat.ChatScreen
import com.example.getjob.presentation.screens.profile.ProfileScreen
import com.example.getjob.presentation.screens.profile.ClientProfileScreen
import com.example.getjob.presentation.viewmodel.LoginViewModel
import com.example.getjob.utils.PreferencesManager
import com.example.getjob.utils.AuthEventBus
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register/{role}") {
        fun createRoute(role: String) = "register/$role"
    }
    object Dashboard : Screen("dashboard") // Dashboard de trabajador
    object ClientDashboard : Screen("client_dashboard") // Dashboard de cliente
    object CreateJob : Screen("create_job") // Crear trabajo (cliente)
    object SolicitudDetail : Screen("solicitud_detail/{jobId}/{applicationId}") {
        fun createRoute(jobId: Int, applicationId: Int? = null): String {
            val appSegment = applicationId ?: -1
            return "solicitud_detail/$jobId/$appSegment"
        }
    }
    object JobFlow : Screen("job_flow/{jobId}") {
        fun createRoute(jobId: Int) = "job_flow/$jobId"
    }
    // Pantallas antiguas eliminadas - ahora se usa JobFlowScreen para todo el flujo
    object PendingCommissions : Screen("pending_commissions")
    object CompleteProfile : Screen("complete_profile") // Completar perfil de trabajador
    object WorkerRequests : Screen("worker_requests") // Solicitudes (trabajos aceptados)
    object Profile : Screen("profile") // Perfil de trabajador
    object ClientProfile : Screen("client_profile") // Perfil de cliente
    object EditClientProfile : Screen("edit_client_profile") // Editar perfil de cliente
    object Chat : Screen("chat/{jobId}/{applicationId}") {
        fun createRoute(jobId: Int, applicationId: Int? = null): String {
            val applicationSegment = applicationId ?: -1
            return "chat/$jobId/$applicationSegment"
        }
    }
    object ClientRateWorker : Screen("client_rate_worker/{jobId}") {
        fun createRoute(jobId: Int) = "client_rate_worker/$jobId"
    }
}

@Composable
fun NavGraph(navController: NavHostController, startDestination: String = Screen.Login.route) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    
    // userRole: estado reactivo en memoria para esta sesión de Compose
    // Se actualiza cuando hay cambios de login y se usa para lógica de UI reactiva
    // preferencesManager: fuente de verdad persistente en disco
    // Se usa cuando necesitamos el valor actualizado desde almacenamiento (redirectToCorrectDashboard, validaciones)
    var userRole by remember { mutableStateOf(preferencesManager.getUserRole()) }
    val loginViewModel: LoginViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as android.app.Application
        )
    )
    
    // ViewModel compartido de WorkerRequests - se crea dentro del composable para mejor scope
    val loginUiState by loginViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    // Obtener la ruta actual para determinar si mostrar la barra de navegación
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    
    // Determinar si mostrar la barra de navegación (solo para pantallas principales de worker)
    val showBottomBar = when (currentRoute) {
        Screen.Dashboard.route,
        Screen.WorkerRequests.route,
        Screen.Profile.route,
        Screen.PendingCommissions.route -> userRole == "worker"
        else -> false
    }
    
    // Estado del perfil (isProfileComplete) - reactivo y actualizado dinámicamente
    var isProfileComplete by remember { mutableStateOf(true) }
    
    // Nota de arquitectura: workerRepository está en el composable por simplicidad.
    // Si la lógica de perfil crece, considerar mover updateProfileStatus() a un ViewModel
    // y usar inyección de dependencias para el repositorio.
    val workerRepository = remember { WorkerRepository() }
    
    // Función para actualizar el estado del perfil
    // Solo marca como incompleto si es 404 (perfil no existe), no por errores de red/servidor
    fun updateProfileStatus() {
        if (userRole == "worker") {
            coroutineScope.launch {
                workerRepository.getMyProfile()
                    .onSuccess { 
                        isProfileComplete = true
                    }
                    .onFailure { exception ->
                        // Solo marcar como incompleto si es 404 (perfil realmente no existe)
                        // Para otros errores (red, servidor, timeout, etc.) mantener el valor actual
                        val errorMessage = exception.message ?: ""
                        val is404 = errorMessage.contains("404") || 
                                   errorMessage.contains("Not Found", ignoreCase = true)
                        
                        // Verificar si es un error de red (no debería marcar como incompleto)
                        val isNetworkError = exception is java.net.SocketTimeoutException ||
                                           exception is java.net.ConnectException ||
                                           exception is java.net.UnknownHostException ||
                                           exception is java.io.IOException ||
                                           errorMessage.contains("conexión", ignoreCase = true) ||
                                           errorMessage.contains("timeout", ignoreCase = true) ||
                                           errorMessage.contains("conect", ignoreCase = true)
                        
                        // Solo marcar como incompleto si es 404 y NO es error de red
                        if (is404 && !isNetworkError) {
                            isProfileComplete = false
                        }
                        // Si no es 404 o es error de red, mantener el valor actual (no cambiar isProfileComplete)
                    }
            }
        }
    }
    
    // Cargar el estado del perfil cuando se navega a pantallas principales de worker
    // Consolidado en un solo LaunchedEffect para evitar duplicación
    LaunchedEffect(currentRoute, userRole) {
        if (userRole == "worker" &&
            (currentRoute == Screen.Dashboard.route ||
             currentRoute == Screen.Profile.route ||
             currentRoute == Screen.WorkerRequests.route ||
             currentRoute == Screen.PendingCommissions.route)
        ) {
            updateProfileStatus()
        }
    }
    
    // Actualizar userRole cuando el login sea exitoso
    LaunchedEffect(loginUiState.isSuccess, loginUiState.userRole) {
        if (loginUiState.isSuccess && loginUiState.userRole != null) {
            userRole = loginUiState.userRole
        }
    }
    
    // Actualizar userRole desde preferencesManager al iniciar el composable (una sola vez)
    // Nota: LaunchedEffect(Unit) se ejecuta una sola vez al entrar en el composable, no periódicamente
    LaunchedEffect(Unit) {
        userRole = preferencesManager.getUserRole()
    }
    
    // Escuchar eventos de autenticación (token expirado, logout)
    LaunchedEffect(Unit) {
        AuthEventBus.authEvents.collectLatest { event ->
            when (event) {
                is AuthEventBus.AuthEvent.TokenExpired -> {
                    // Redirigir al login cuando el token expire
                    android.util.Log.d("NavGraph", "Token expirado, redirigiendo al login")
                    navController.navigate(Screen.Login.route) {
                        // Limpiar todo el back stack
                        popUpTo(0) { inclusive = true }
                    }
                }
                is AuthEventBus.AuthEvent.Logout -> {
                    // Redirigir al login cuando haya logout
                    android.util.Log.d("NavGraph", "Logout, redirigiendo al login")
                    navController.navigate(Screen.Login.route) {
                        // Limpiar todo el back stack
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }
    
    // Función helper para redirigir según rol
    // Usa preferencesManager (fuente de verdad en disco) en lugar de userRole (estado en memoria)
    // para asegurar que tenemos el valor más actualizado al redirigir
    fun redirectToCorrectDashboard() {
        val currentRole = preferencesManager.getUserRole()
        android.util.Log.d("NavGraph", "redirectToCorrectDashboard - currentRole: $currentRole")
        if (currentRole == null || currentRole.isEmpty()) {
            // Si no hay rol, ir al login
            android.util.Log.d("NavGraph", "No hay rol, redirigiendo al login")
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        } else if (currentRole == "client") {
            android.util.Log.d("NavGraph", "Redirigiendo a ClientDashboard")
            navController.navigate(Screen.ClientDashboard.route) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            android.util.Log.d("NavGraph", "Redirigiendo a Dashboard (worker)")
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    // La navegación después del login se maneja en el callback onLoginSuccess del LoginScreen
    
    // Determinar currentRoute para la barra de navegación (verificar que coincida exactamente con Screen.*.route)
    val bottomBarCurrentRoute = when (currentRoute) {
        Screen.Dashboard.route -> "dashboard" // Screen.Dashboard.route = "dashboard" ✓
        Screen.WorkerRequests.route -> "requests" // Screen.WorkerRequests.route = "worker_requests", pero el componente espera "requests" ✓
        Screen.Profile.route -> "profile" // Screen.Profile.route = "profile" ✓
        Screen.PendingCommissions.route -> "commissions" // Screen.PendingCommissions.route = "pending_commissions", pero el componente espera "commissions" ✓
        else -> ""
    }
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                WorkerBottomNavigationBar(
                    isProfileComplete = isProfileComplete,
                    onProfileClick = {
                        if (isProfileComplete) {
                            navController.navigate(Screen.Profile.route) {
                                launchSingleTop = true
                                popUpTo(Screen.Dashboard.route) { inclusive = false }
                                restoreState = true
                            }
                        } else {
                            navController.navigate(Screen.CompleteProfile.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onNavigateToDashboard = {
                        navController.navigate(Screen.Dashboard.route) {
                            launchSingleTop = true
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                            restoreState = true
                        }
                    },
                    onNavigateToRequests = {
                        navController.navigate(Screen.WorkerRequests.route) {
                            launchSingleTop = true
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                            restoreState = true
                        }
                    },
                    onNavigateToCommissions = {
                        navController.navigate(Screen.PendingCommissions.route) {
                            launchSingleTop = true
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                            restoreState = true
                        }
                    },
                    currentRoute = bottomBarCurrentRoute
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { role ->
                    navController.navigate(Screen.Register.createRoute(role))
                },
                onLoginSuccess = { loggedInRole ->
                    // Actualizar userRole reactivo
                    userRole = loggedInRole
                    // Navegar según el rol del usuario y limpiar el back stack
                    if (loggedInRole == "client") {
                        navController.navigate(Screen.ClientDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable(
            route = Screen.Register.route,
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "worker"
            if (role == "worker") {
                val context = LocalContext.current
                val preferencesManager = remember { PreferencesManager(context) }
                
                RegisterWorkerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onRegisterSuccess = { newRole ->
                        userRole = newRole
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onProfileComplete = {
                        preferencesManager.setProfileCreatedFirstTime(true)
                        userRole = "worker"
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            } else {
                RegisterClientScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onRegisterSuccess = { userRole ->
                        navController.navigate(Screen.ClientDashboard.route)
                    }
                )
            }
        }
        
        composable(Screen.Dashboard.route) { backStackEntry ->
            // Crear ViewModel con backStackEntry para que persista entre navegaciones
            val dashboardViewModel: com.example.getjob.presentation.viewmodel.DashboardViewModel = 
                androidx.lifecycle.viewmodel.compose.viewModel(backStackEntry)
            
            // Validar que es trabajador - mostrar loading mientras redirige para evitar pantalla blanca
            val currentRole = remember { preferencesManager.getUserRole() }
            android.util.Log.d("NavGraph", "Dashboard - userRole: $userRole, currentRole: $currentRole")
            
            if (userRole != "worker" && currentRole != "worker") {
                LaunchedEffect(Unit) {
                    android.util.Log.d("NavGraph", "Dashboard - Redirigiendo porque no es worker")
                    redirectToCorrectDashboard()
                }
                // Mostrar loading mientras redirige
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                com.example.getjob.presentation.screens.dashboard.DashboardScreen(
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToJobDetail = { jobId ->
                    navController.navigate(Screen.SolicitudDetail.createRoute(jobId))
                },
                onJobAccepted = { jobId ->
                    // Navegar a la pantalla de detalles del trabajo después de aceptarlo
                    navController.navigate(Screen.SolicitudDetail.createRoute(jobId))
                },
                onNavigateToCompleteProfile = {
                    navController.navigate(Screen.CompleteProfile.route)
                },
                onNavigateToRequests = {
                    // Navegar a WorkerRequests después de aplicar a un trabajo
                    // Mantener el Dashboard en el back stack para poder volver
                    navController.navigate(Screen.WorkerRequests.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                    }
                },
                onNavigateToCommissions = {
                    navController.navigate(Screen.PendingCommissions.route)
                },
                onLogout = {
                    // 1. Limpiar datos locales de autenticación
                    preferencesManager.clearAuthData()
                    
                    // 2. Actualizar userRole en memoria para que las validaciones funcionen
                    userRole = null
                    
                    // 3. Emitir evento global de logout (consistente con TokenExpired)
                    coroutineScope.launch {
                        AuthEventBus.emitLogout()
                    }
                    
                    // 4. Navegar al login y limpiar back stack
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                viewModel = dashboardViewModel
                )
            }
        }
        
        composable(Screen.Profile.route) {
            // Validar que es trabajador
            if (userRole != "worker") {
                LaunchedEffect(Unit) {
                    redirectToCorrectDashboard()
                }
                return@composable
            }
            
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.CompleteProfile.route)
                }
            )
        }
        
        composable(Screen.WorkerRequests.route) { backStackEntry ->
            // Validar que es trabajador
            if (userRole != "worker") {
                LaunchedEffect(Unit) {
                    redirectToCorrectDashboard()
                }
                return@composable
            }
            
            // ViewModel scoped a esta entrada del back stack
            val workerRequestsViewModel: com.example.getjob.presentation.viewmodel.WorkerRequestsViewModel = 
                androidx.lifecycle.viewmodel.compose.viewModel(backStackEntry)
            
            WorkerRequestsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToJobDetail = { jobId, applicationId ->
                    navController.navigate(Screen.SolicitudDetail.createRoute(jobId, applicationId))
                },
                onNavigateToChat = { jobId, applicationId ->
                    navController.navigate(Screen.Chat.createRoute(jobId, applicationId))
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        // Limpiar el back stack hasta el dashboard
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                    }
                },
                onNavigateToCommissions = {
                    navController.navigate(Screen.PendingCommissions.route)
                },
                viewModel = workerRequestsViewModel
            )
        }
        
        composable(Screen.CompleteProfile.route) {
            // Validar que es trabajador
            if (userRole != "worker") {
                LaunchedEffect(Unit) {
                    redirectToCorrectDashboard()
                }
                return@composable
            }
            
            val context = LocalContext.current
            val preferencesManager = remember { com.example.getjob.utils.PreferencesManager(context) }
            
            RegisterWorkerScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = { /* No usado cuando initialStep = 2 */ },
                initialStep = 2, // Empezar directamente en el paso 2 (perfil)
                onProfileComplete = {
                    // Marcar que el perfil se acaba de crear por primera vez
                    preferencesManager.setProfileCreatedFirstTime(true)
                    // Actualizar el estado del perfil inmediatamente
                    isProfileComplete = true
                    // Volver al dashboard (el dashboard recargará el estado del perfil automáticamente)
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.ClientDashboard.route) {
            // Validar que es cliente
            if (userRole != "client") {
                LaunchedEffect(Unit) {
                    redirectToCorrectDashboard()
                }
                return@composable
            }
            
            ClientDashboardScreen(
                onNavigateToCreateJob = { 
                    navController.navigate(Screen.CreateJob.route) 
                },
                onNavigateToJobDetail = { jobId ->
                    navController.navigate(Screen.SolicitudDetail.createRoute(jobId))
                },
                onNavigateToChat = { jobId, applicationId ->
                    navController.navigate(Screen.Chat.createRoute(jobId, applicationId))
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.ClientProfile.route)
                },
                onLogout = {
                    // 1. Limpiar datos locales de autenticación
                    preferencesManager.clearAuthData()
                    
                    // 2. Actualizar userRole en memoria para que las validaciones funcionen
                    userRole = null
                    
                    // 3. Emitir evento global de logout (consistente con TokenExpired)
                    coroutineScope.launch {
                        AuthEventBus.emitLogout()
                    }
                    
                    // 4. Navegar al login y limpiar back stack
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.CreateJob.route) {
            // Validar que es cliente
            if (userRole != "client") {
                LaunchedEffect(Unit) {
                    redirectToCorrectDashboard()
                }
                return@composable
            }
            
            CreateJobScreen(
                onNavigateBack = { navController.popBackStack() },
                onJobCreated = {
                    navController.popBackStack()
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.ClientDashboard.route) {
                        popUpTo(Screen.ClientDashboard.route) { inclusive = false }
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.ClientProfile.route)
                }
            )
        }
        
        composable(Screen.ClientProfile.route) {
            // Validar que es cliente
            if (userRole != "client") {
                LaunchedEffect(Unit) {
                    redirectToCorrectDashboard()
                }
                return@composable
            }
            
            ClientProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDashboard = {
                    navController.navigate(Screen.ClientDashboard.route) {
                        popUpTo(Screen.ClientDashboard.route) { inclusive = false }
                    }
                },
                onNavigateToCreateJob = {
                    navController.navigate(Screen.CreateJob.route)
                },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditClientProfile.route)
                }
            )
        }
        
        composable(Screen.EditClientProfile.route) {
            // Validar que es cliente
            if (userRole != "client") {
                LaunchedEffect(Unit) {
                    redirectToCorrectDashboard()
                }
                return@composable
            }
            
            com.example.getjob.presentation.screens.profile.EditClientProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.SolicitudDetail.route,
            arguments = listOf(
                navArgument("jobId") { type = NavType.IntType },
                navArgument("applicationId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getInt("jobId") ?: 0
            val applicationIdArg = backStackEntry.arguments?.getInt("applicationId") ?: -1
            val applicationId = applicationIdArg.takeIf { it > 0 }
            
            // Validar acceso al trabajo (cliente o trabajador asignado)
            var hasAccess by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(jobId) {
                if (jobId > 0) {
                    val userId = preferencesManager.getUserId()
                    val currentRole = preferencesManager.getUserRole()
                    val jobResult = com.example.getjob.data.repository.JobRepository().getJobById(jobId)
                    jobResult.onSuccess { job ->
                        if (currentRole == "client") {
                            hasAccess = job.client_id == userId
                        } else if (currentRole == "worker") {
                            // Obtener el worker_id del usuario actual
                            val workerRepository = com.example.getjob.data.repository.WorkerRepository()
                            val workerResult = workerRepository.getMyProfile()
                            workerResult.onSuccess { worker ->
                                // El trabajador puede ver trabajos disponibles (worker_id == null) o asignados a él
                                hasAccess = job.worker_id == null || job.worker_id == worker.id
                            }.onFailure {
                                // Si no puede obtener el perfil, solo puede ver trabajos disponibles
                                hasAccess = job.worker_id == null
                            }
                        } else {
                            hasAccess = false
                        }
                    }.onFailure {
                        hasAccess = false
                    }
                }
            }
            
            if (hasAccess == false) {
                LaunchedEffect(Unit) {
                    redirectToCorrectDashboard()
                }
                return@composable
            }
            
            if (hasAccess == true) {
                SolicitudDetailScreen(
                    jobId = jobId,
                    applicationId = applicationId,
                    onNavigateBack = { navController.popBackStack() },
                    onStartService = {
                        // Callback legacy, mantener por compatibilidad
                    },
                    onChat = { chatJobId, chatApplicationId ->
                        navController.navigate(Screen.Chat.createRoute(chatJobId, chatApplicationId))
                    },
                    onNavigateToOnRoute = { jobId ->
                        navController.navigate(Screen.JobFlow.createRoute(jobId))
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToCompleteProfile = {
                        navController.navigate(Screen.CompleteProfile.route)
                    },
                    onNavigateToRequests = {
                        navController.navigate(Screen.WorkerRequests.route)
                    },
                    onNavigateToDashboard = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                        }
                    },
                    onNavigateToCommissions = {
                        navController.navigate(Screen.PendingCommissions.route)
                    },
                    onJobCancelled = {
                        // Nota: El ViewModel de WorkerRequests ya tiene lógica para refrescar
                        // cuando sea necesario. Si el usuario navega a WorkerRequests después,
                        // el ViewModel se refrescará automáticamente o el usuario puede usar
                        // el botón de refresh manual en el TopAppBar.
                        // No necesitamos acceder al back stack aquí ya que backQueue es privado.
                    },
                    onNavigateToRateWorker = { jobId ->
                        navController.navigate(Screen.ClientRateWorker.createRoute(jobId))
                    },
                    onNavigateToClientDashboard = {
                        navController.navigate(Screen.ClientDashboard.route) {
                            popUpTo(Screen.ClientDashboard.route) { inclusive = false }
                        }
                    },
                    onNavigateToClientProfile = {
                        navController.navigate(Screen.ClientProfile.route)
                    },
                    onNavigateToCreateJob = {
                        navController.navigate(Screen.CreateJob.route)
                    }
                )
            }
        }
        
        composable(
            route = Screen.ClientRateWorker.route,
            arguments = listOf(navArgument("jobId") { type = NavType.IntType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getInt("jobId") ?: 0
            
            // Validar que es cliente
            if (userRole != "client") {
                LaunchedEffect(Unit) {
                    redirectToCorrectDashboard()
                }
                return@composable
            }
            
            // Validar que el trabajo pertenece al cliente
            var hasAccess by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(jobId) {
                if (jobId > 0) {
                    val userId = preferencesManager.getUserId()
                    val jobResult = com.example.getjob.data.repository.JobRepository().getJobById(jobId)
                    jobResult.onSuccess { job ->
                        hasAccess = job.client_id == userId
                    }.onFailure {
                        hasAccess = false
                    }
                }
            }
            
            if (hasAccess == false) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.ClientDashboard.route) {
                        popUpTo(Screen.ClientDashboard.route) { inclusive = false }
                    }
                }
                return@composable
            }
            
            if (hasAccess == true) {
                ClientRateWorkerScreen(
                jobId = jobId,
                onNavigateBack = { navController.popBackStack() },
                onRatingSubmitted = {
                    // Volver al detalle del trabajo después de calificar
                    navController.popBackStack()
                }
            )
            }
        }
        
        composable(
            route = Screen.JobFlow.route,
            arguments = listOf(navArgument("jobId") { type = NavType.IntType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getInt("jobId") ?: 0
            
            // Validar que es trabajador
            if (userRole != "worker") {
                LaunchedEffect(Unit) {
                    redirectToCorrectDashboard()
                }
                return@composable
            }
            
            // Validar acceso al trabajo (solo una vez, no resetear al navegar)
            var hasAccess by remember(jobId) { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(jobId) {
                if (jobId > 0 && hasAccess == null) {
                    val userId = preferencesManager.getUserId()
                    val jobResult = com.example.getjob.data.repository.JobRepository().getJobById(jobId)
                    jobResult.onSuccess { job ->
                        hasAccess = job.worker_id != null
                    }.onFailure {
                        hasAccess = false
                    }
                } else if (jobId <= 0) {
                    hasAccess = false
                }
            }
            
            // Crear ViewModel con backStackEntry para que persista entre navegaciones
            val jobFlowViewModel: com.example.getjob.presentation.viewmodel.JobFlowViewModel = 
                androidx.lifecycle.viewmodel.compose.viewModel(backStackEntry)
            
            // Si no tiene acceso, redirigir pero mostrar el screen para que la navegación funcione
            if (hasAccess == false) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
            
            // Mostrar el screen siempre, incluso durante la validación de acceso
            // El JobFlowScreen manejará su propio estado de carga
            JobFlowScreen(
                jobId = jobId,
                viewModel = jobFlowViewModel,
                onNavigateBack = {
                    // Navegación simple: siempre volver atrás
                    navController.popBackStack()
                },
                onNavigateToChat = { chatJobId ->
                    navController.navigate(Screen.Chat.createRoute(chatJobId))
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                    }
                }
            )
        }
        
        // ============================================
        // PANTALLAS ANTIGUAS ELIMINADAS
        // OnRouteScreen, OnSiteScreen, ServiceInProgressScreen, PaymentAndReviewScreen
        // Ahora todo el flujo se maneja en JobFlowScreen
        // ============================================
        
        composable(Screen.PendingCommissions.route) {
            // Validar que es trabajador
            if (userRole != "worker") {
                LaunchedEffect(Unit) {
                    redirectToCorrectDashboard()
                }
                return@composable
            }
            
            PendingCommissionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onPaymentSubmitted = {
                    // TODO: Recargar comisiones y volver al dashboard si no hay más pendientes
                    navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                }
            )
        }
        
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("jobId") { type = NavType.IntType },
                navArgument("applicationId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getInt("jobId") ?: 0
            val applicationIdArg = backStackEntry.arguments?.getInt("applicationId") ?: -1
            val applicationId = applicationIdArg.takeIf { it > 0 }
            com.example.getjob.presentation.screens.chat.ChatScreen(
                jobId = jobId,
                applicationId = applicationId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        }
    }
}

