package com.example.getjob.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Utilidades para hacer la UI responsive según el tamaño de pantalla
 */

@Composable
fun getScreenWidth(): Int {
    return LocalConfiguration.current.screenWidthDp
}

@Composable
fun getScreenHeight(): Int {
    return LocalConfiguration.current.screenHeightDp
}

/**
 * Determina si es una pantalla pequeña (teléfono)
 */
@Composable
fun isSmallScreen(): Boolean {
    return getScreenWidth() < 600
}

/**
 * Determina si es una pantalla mediana (tablet pequeña)
 */
@Composable
fun isMediumScreen(): Boolean {
    val width = getScreenWidth()
    return width >= 600 && width < 840
}

/**
 * Determina si es una pantalla grande (tablet grande o desktop)
 */
@Composable
fun isLargeScreen(): Boolean {
    return getScreenWidth() >= 840
}

/**
 * Padding horizontal adaptativo según el tamaño de pantalla
 */
@Composable
fun responsiveHorizontalPadding(): Dp {
    return when {
        isLargeScreen() -> 32.dp
        isMediumScreen() -> 24.dp
        else -> 16.dp
    }
}

/**
 * Padding vertical adaptativo según el tamaño de pantalla
 */
@Composable
fun responsiveVerticalPadding(): Dp {
    return when {
        isLargeScreen() -> 24.dp
        isMediumScreen() -> 20.dp
        else -> 16.dp
    }
}

/**
 * Spacing adaptativo entre elementos
 */
@Composable
fun responsiveSpacing(): Dp {
    return when {
        isLargeScreen() -> 24.dp
        isMediumScreen() -> 20.dp
        else -> 16.dp
    }
}

/**
 * Tamaño de fuente para títulos grandes adaptativo
 */
@Composable
fun responsiveTitleFontSize(): TextUnit {
    return when {
        isLargeScreen() -> 28.sp
        isMediumScreen() -> 24.sp
        else -> 20.sp
    }
}

/**
 * Tamaño de fuente para títulos medianos adaptativo
 */
@Composable
fun responsiveSubtitleFontSize(): TextUnit {
    return when {
        isLargeScreen() -> 20.sp
        isMediumScreen() -> 18.sp
        else -> 16.sp
    }
}

/**
 * Tamaño de fuente para texto del cuerpo adaptativo
 */
@Composable
fun responsiveBodyFontSize(): TextUnit {
    return when {
        isLargeScreen() -> 16.sp
        isMediumScreen() -> 15.sp
        else -> 14.sp
    }
}

/**
 * Ancho máximo para contenido en pantallas grandes (centrado)
 */
@Composable
fun responsiveMaxContentWidth(): Dp {
    return when {
        isLargeScreen() -> 1200.dp
        isMediumScreen() -> 800.dp
        else -> Dp.Unspecified
    }
}

/**
 * Tamaño de icono adaptativo
 */
@Composable
fun responsiveIconSize(): Dp {
    return when {
        isLargeScreen() -> 32.dp
        isMediumScreen() -> 28.dp
        else -> 24.dp
    }
}

/**
 * Tamaño de avatar/foto de perfil adaptativo
 */
@Composable
fun responsiveAvatarSize(): Dp {
    return when {
        isLargeScreen() -> 160.dp
        isMediumScreen() -> 140.dp
        else -> 120.dp
    }
}

/**
 * Radio de esquinas adaptativo para cards
 */
@Composable
fun responsiveCardCornerRadius(): Dp {
    return when {
        isLargeScreen() -> 40.dp
        isMediumScreen() -> 36.dp
        else -> 32.dp
    }
}

