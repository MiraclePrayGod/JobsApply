package com.example.getjob

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.getjob.data.api.ApiClient
import com.example.getjob.presentation.navigation.NavGraph
import com.example.getjob.presentation.navigation.Screen
import com.example.getjob.ui.theme.GetJobTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Inicializar ApiClient con el contexto
        ApiClient.initialize(this)
        
        setContent {
            GetJobTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val preferencesManager = remember { com.example.getjob.utils.PreferencesManager(context) }
                    
                    // Recordar sesión del usuario
                    val initialDestination = remember {
                        val isLoggedIn = preferencesManager.isLoggedIn()
                        val token = preferencesManager.getToken()
                        val role = preferencesManager.getUserRole()
                        
                        if (isLoggedIn && token != null && token.isNotBlank() && role != null) {
                            // Verificar que el token sea válido haciendo una petición al backend
                            // Por ahora, confiamos en que si hay token guardado, es válido
                            // El interceptor manejará tokens expirados automáticamente
                            if (role == "worker") Screen.Dashboard.route
                            else if (role == "client") Screen.ClientDashboard.route
                            else Screen.Login.route
                        } else {
                            Screen.Login.route
                        }
                    }
                    
                    NavGraph(
                        navController = navController,
                        startDestination = initialDestination
                    )
                }
            }
        }
    }
}