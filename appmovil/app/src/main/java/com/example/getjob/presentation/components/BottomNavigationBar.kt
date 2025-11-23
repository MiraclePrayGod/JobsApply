package com.example.getjob.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.getjob.presentation.screens.register.RegisterColors

/**
 * Barra de navegación inferior para TRABAJADORES
 */
@Composable
fun WorkerBottomNavigationBar(
    isProfileComplete: Boolean = true,
    onProfileClick: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToRequests: () -> Unit = {},
    onNavigateToCommissions: () -> Unit = {},
    currentRoute: String = "dashboard"
) {
    Box {
        // Línea separadora arriba del bottom bar
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = RegisterColors.BorderGray,
            thickness = 1.dp
        )
        NavigationBar(
            containerColor = RegisterColors.White,
            contentColor = RegisterColors.DarkGray
        ) {
        // Inicio
        NavigationBarItem(
            selected = currentRoute == "dashboard",
            onClick = onNavigateToDashboard,
            icon = {
                Icon(
                    Icons.Outlined.Home,
                    contentDescription = "Inicio",
                    tint = if (currentRoute == "dashboard") RegisterColors.DarkGray else RegisterColors.TextGray
                )
            },
            label = {
                Text(
                    "Inicio",
                    color = if (currentRoute == "dashboard") RegisterColors.DarkGray else RegisterColors.TextGray
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = RegisterColors.DarkGray,
                selectedTextColor = RegisterColors.DarkGray,
                unselectedIconColor = RegisterColors.TextGray,
                unselectedTextColor = RegisterColors.TextGray,
                indicatorColor = Color.Transparent
            )
        )
        
        // Solicitudes
        NavigationBarItem(
            selected = currentRoute == "requests",
            onClick = onNavigateToRequests,
            icon = {
                Icon(
                    Icons.Outlined.Assignment,
                    contentDescription = "Solicitudes",
                    tint = if (currentRoute == "requests") RegisterColors.DarkGray else RegisterColors.TextGray
                )
            },
            label = {
                Text(
                    "Solicitudes",
                    color = if (currentRoute == "requests") RegisterColors.DarkGray else RegisterColors.TextGray
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = RegisterColors.DarkGray,
                selectedTextColor = RegisterColors.DarkGray,
                unselectedIconColor = RegisterColors.TextGray,
                unselectedTextColor = RegisterColors.TextGray,
                indicatorColor = Color.Transparent
            )
        )
        
        // Perfil
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = onProfileClick,
            icon = {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = "Perfil",
                    tint = if (isProfileComplete && currentRoute == "profile") {
                        RegisterColors.DarkGray
                    } else if (!isProfileComplete) {
                        RegisterColors.PrimaryOrange
                    } else {
                        RegisterColors.TextGray
                    }
                )
            },
            label = {
                Text(
                    "Perfil",
                    color = if (isProfileComplete && currentRoute == "profile") {
                        RegisterColors.DarkGray
                    } else if (!isProfileComplete) {
                        RegisterColors.PrimaryOrange
                    } else {
                        RegisterColors.TextGray
                    }
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = RegisterColors.DarkGray,
                selectedTextColor = RegisterColors.DarkGray,
                unselectedIconColor = if (!isProfileComplete) RegisterColors.PrimaryOrange else RegisterColors.TextGray,
                unselectedTextColor = if (!isProfileComplete) RegisterColors.PrimaryOrange else RegisterColors.TextGray,
                indicatorColor = Color.Transparent
            )
        )
        
        // Modo Plus
        NavigationBarItem(
            selected = currentRoute == "commissions",
            onClick = onNavigateToCommissions,
            icon = {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Modo Plus",
                    tint = if (currentRoute == "commissions") RegisterColors.DarkGray else RegisterColors.TextGray
                )
            },
            label = {
                Text(
                    "Modo Plus",
                    color = if (currentRoute == "commissions") RegisterColors.DarkGray else RegisterColors.TextGray
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = RegisterColors.DarkGray,
                selectedTextColor = RegisterColors.DarkGray,
                unselectedIconColor = RegisterColors.TextGray,
                unselectedTextColor = RegisterColors.TextGray,
                indicatorColor = Color.Transparent
            )
        )
        }
    }
}

/**
 * Barra de navegación inferior para CLIENTES
 */
@Composable
fun ClientBottomNavigationBar(
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToCreateJob: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    currentRoute: String = "dashboard"
) {
    Box {
        // Línea separadora arriba del bottom bar
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = RegisterColors.BorderGray,
            thickness = 1.dp
        )
        NavigationBar(
            containerColor = RegisterColors.White,
            contentColor = RegisterColors.DarkGray
        ) {
        // Inicio (Mis Trabajos)
        NavigationBarItem(
            selected = currentRoute == "dashboard",
            onClick = onNavigateToDashboard,
            icon = { 
                Icon(
                    Icons.Outlined.Home, 
                    contentDescription = "Inicio",
                    tint = if (currentRoute == "dashboard") RegisterColors.DarkGray else RegisterColors.TextGray
                ) 
            },
            label = { 
                Text(
                    "Inicio",
                    color = if (currentRoute == "dashboard") RegisterColors.DarkGray else RegisterColors.TextGray,
                    fontSize = 12.sp
                ) 
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = RegisterColors.DarkGray,
                selectedTextColor = RegisterColors.DarkGray,
                unselectedIconColor = RegisterColors.TextGray,
                unselectedTextColor = RegisterColors.TextGray,
                indicatorColor = Color.Transparent
            )
        )
        
        // Crear Trabajo
        NavigationBarItem(
            selected = currentRoute == "create_job",
            onClick = onNavigateToCreateJob,
            icon = { 
                Icon(
                    Icons.Outlined.AddCircle, 
                    contentDescription = "Crear trabajo",
                    tint = if (currentRoute == "create_job") RegisterColors.DarkGray else RegisterColors.TextGray
                ) 
            },
            label = { 
                Text(
                    "Crear",
                    color = if (currentRoute == "create_job") RegisterColors.DarkGray else RegisterColors.TextGray,
                    fontSize = 12.sp
                ) 
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = RegisterColors.DarkGray,
                selectedTextColor = RegisterColors.DarkGray,
                unselectedIconColor = RegisterColors.TextGray,
                unselectedTextColor = RegisterColors.TextGray,
                indicatorColor = Color.Transparent
            )
        )
        
        // Perfil
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = onNavigateToProfile,
            icon = { 
                Icon(
                    Icons.Outlined.Person, 
                    contentDescription = "Perfil",
                    tint = if (currentRoute == "profile") RegisterColors.DarkGray else RegisterColors.TextGray
                ) 
            },
            label = { 
                Text(
                    "Perfil",
                    color = if (currentRoute == "profile") RegisterColors.DarkGray else RegisterColors.TextGray,
                    fontSize = 12.sp
                ) 
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = RegisterColors.DarkGray,
                selectedTextColor = RegisterColors.DarkGray,
                unselectedIconColor = RegisterColors.TextGray,
                unselectedTextColor = RegisterColors.TextGray,
                indicatorColor = Color.Transparent
            )
        )
        }
    }
}

