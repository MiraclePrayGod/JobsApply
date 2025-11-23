package com.example.getjob.presentation.components

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.getjob.presentation.screens.register.RegisterColors
import com.example.getjob.utils.LocationService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {},
    showDialog: Boolean = true
) {
    val context = LocalContext.current
    val locationService = remember { LocationService.getInstance(context) }
    
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }
    var hasCheckedPermissions by remember { mutableStateOf(false) }
    
    // Verificar permisos al montar
    LaunchedEffect(Unit) {
        if (!hasCheckedPermissions) {
            hasCheckedPermissions = true
            when {
                permissionsState.allPermissionsGranted -> {
                    // Verificar si GPS está habilitado
                    if (locationService.isGpsEnabled()) {
                        onPermissionGranted()
                    } else {
                        showGpsDialog = true
                    }
                }
                permissionsState.shouldShowRationale -> {
                    if (showDialog) {
                        showRationaleDialog = true
                    } else {
                        onPermissionDenied()
                    }
                }
                else -> {
                    if (showDialog) {
                        permissionsState.launchMultiplePermissionRequest()
                    } else {
                        onPermissionDenied()
                    }
                }
            }
        }
    }
    
    // Manejar resultado de permisos
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted && hasCheckedPermissions) {
            if (locationService.isGpsEnabled()) {
                onPermissionGranted()
            } else {
                showGpsDialog = true
            }
        } else if (!permissionsState.allPermissionsGranted && hasCheckedPermissions && !permissionsState.shouldShowRationale) {
            // Si los permisos no están otorgados, no se debe mostrar rationale, y ya se verificaron, entonces fueron denegados
            onPermissionDenied()
        }
    }
    
    // Diálogo explicativo de permisos
    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { 
                showRationaleDialog = false
                onPermissionDenied()
            },
            title = {
                Text(
                    text = "Permisos de Ubicación",
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.DarkGray
                )
            },
            text = {
                Text(
                    text = "Necesitamos acceso a tu ubicación para mostrarte en el mapa y calcular la ruta al cliente. Esto solo se usa cuando estás en ruta.",
                    color = RegisterColors.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRationaleDialog = false
                        permissionsState.launchMultiplePermissionRequest()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RegisterColors.PrimaryOrange
                    )
                ) {
                    Text("Permitir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRationaleDialog = false
                        onPermissionDenied()
                    }
                ) {
                    Text("Cancelar", color = RegisterColors.TextGray)
                }
            },
            containerColor = RegisterColors.White
        )
    }
    
    // Diálogo para activar GPS
    if (showGpsDialog) {
        AlertDialog(
            onDismissRequest = { 
                showGpsDialog = false
                onPermissionDenied()
            },
            title = {
                Text(
                    text = "Activar GPS",
                    fontWeight = FontWeight.Bold,
                    color = RegisterColors.DarkGray
                )
            },
            text = {
                Text(
                    text = "Por favor, activa el GPS en la configuración de tu dispositivo para poder obtener tu ubicación precisa.",
                    color = RegisterColors.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGpsDialog = false
                        // Abrir configuración de ubicación
                        val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RegisterColors.PrimaryOrange
                    )
                ) {
                    Text("Abrir Configuración")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showGpsDialog = false
                        onPermissionDenied()
                    }
                ) {
                    Text("Cancelar", color = RegisterColors.TextGray)
                }
            },
            containerColor = RegisterColors.White
        )
    }
}

/**
 * Componente simple para solicitar permisos sin diálogo
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestLocationPermission(
    onPermissionResult: (Boolean) -> Unit
) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        onPermissionResult(permissionsState.allPermissionsGranted)
    }
    
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }
}

