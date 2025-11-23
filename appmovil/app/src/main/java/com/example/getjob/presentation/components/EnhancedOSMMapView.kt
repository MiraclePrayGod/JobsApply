package com.example.getjob.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.getjob.presentation.screens.register.RegisterColors
import com.example.getjob.utils.RouteService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import android.location.Location
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import android.graphics.Color as AndroidColor

/**
 * Componente de mapa mejorado que muestra:
 * - Ubicación del trabajador (marcador azul)
 * - Ubicación del cliente (marcador rojo)
 * - Ruta entre ambos puntos (línea)
 */
@Composable
fun EnhancedOSMMapView(
    workerLatitude: Double? = null,
    workerLongitude: Double? = null,
    clientLatitude: Double,
    clientLongitude: Double,
    clientAddress: String = "",
    showRoute: Boolean = true,
    showETA: Boolean = true,
    onStartNavigation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val routeService = remember { RouteService() }
    var routeResult by remember { mutableStateOf<RouteService.RouteResult?>(null) }
    var isLoadingRoute by remember { mutableStateOf(false) }
    
    // Calcular ruta y ETA si hay ubicación del trabajador (separado del mapa)
    LaunchedEffect(workerLatitude, workerLongitude, clientLatitude, clientLongitude, showETA) {
        if (workerLatitude != null && workerLongitude != null && showETA) {
            isLoadingRoute = true
            withContext(Dispatchers.IO) {
                routeResult = routeService.calculateRoute(
                    workerLatitude,
                    workerLongitude,
                    clientLatitude,
                    clientLongitude
                )
            }
            isLoadingRoute = false
        } else {
            routeResult = null
        }
    }
    
    // Configurar OSMDroid
    val mapView = remember {
        // Inicializar OSMDroid (se puede llamar múltiples veces sin problema)
        try {
            Configuration.getInstance().load(
                context,
                context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE)
            )
            Configuration.getInstance().userAgentValue = context.packageName
        } catch (e: Exception) {
            // Si ya está inicializado, ignorar el error
        }
        
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 5.0
            maxZoomLevel = 19.0
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
            isClickable = true
            isFocusable = true
            
            // Asegurar que el mapa tenga tamaño válido
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
    
    // Actualizar mapa cuando cambien las coordenadas
    LaunchedEffect(workerLatitude, workerLongitude, clientLatitude, clientLongitude) {
        withContext(Dispatchers.Main) {
            mapView.overlays.clear()
            
            val clientPoint = GeoPoint(clientLatitude, clientLongitude)
            
            // Crear icono personalizado para cliente (rojo)
            val clientIcon = createCustomMarkerIcon(
                context = context,
                color = Color(0xFFE53935), // Rojo
                size = 60
            )
            
            // Agregar marcador del cliente (rojo)
            val clientMarker = Marker(mapView).apply {
                position = clientPoint
                title = clientAddress.ifEmpty { "Destino" }
                icon = clientIcon
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(clientMarker)
            
            // Si hay ubicación del trabajador, agregar marcador y ruta
            if (workerLatitude != null && workerLongitude != null) {
                val workerPoint = GeoPoint(workerLatitude, workerLongitude)
                
                // Crear icono personalizado para trabajador (azul)
                val workerIcon = createCustomMarkerIcon(
                    context = context,
                    color = Color(0xFF2196F3), // Azul
                    size = 60
                )
                
                // Agregar marcador del trabajador (azul)
                val workerMarker = Marker(mapView).apply {
                    position = workerPoint
                    title = "Mi ubicación"
                    icon = workerIcon
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(workerMarker)
                
                // Dibujar ruta si está habilitado
                if (showRoute) {
                    if (routeResult != null && routeResult!!.polyline.isNotEmpty()) {
                        // Usar ruta calculada de OSRM (ruta real)
                        val routePoints = routeResult!!.polyline.map { (lat, lon) ->
                            GeoPoint(lat, lon)
                        }
                        val route = Polyline().apply {
                            // Agregar puntos uno por uno
                            routePoints.forEach { point ->
                                addPoint(point)
                            }
                            // Usar Paint en lugar de propiedades deprecadas
                            paint.color = AndroidColor.parseColor("#2196F3") // Azul
                            paint.strokeWidth = 12f
                        }
                        mapView.overlays.add(route)
                    } else {
                        // Fallback: línea recta si no se pudo calcular la ruta
                        val route = Polyline().apply {
                            addPoint(workerPoint)
                            addPoint(clientPoint)
                            // Usar Paint en lugar de propiedades deprecadas
                            paint.color = AndroidColor.parseColor("#2196F3") // Azul
                            paint.strokeWidth = 12f
                        }
                        mapView.overlays.add(route)
                    }
                }
                
                // Ajustar zoom para mostrar ambos puntos
                val bounds = BoundingBox(
                    maxOf(workerLatitude, clientLatitude), // maxLatitude (norte)
                    maxOf(workerLongitude, clientLongitude), // maxLongitude (este)
                    minOf(workerLatitude, clientLatitude), // minLatitude (sur)
                    minOf(workerLongitude, clientLongitude) // minLongitude (oeste)
                )
                mapView.zoomToBoundingBox(bounds, true, 50)
            } else {
                // Solo mostrar cliente si no hay ubicación del trabajador
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(clientPoint)
            }
            
            mapView.invalidate()
        }
    }
    
    // Limpiar cuando se desmonte
    DisposableEffect(Unit) {
        onDispose {
            try {
                // El mapa se limpia automáticamente cuando se desmonta
            } catch (e: Exception) {
                // Ignorar errores
            }
        }
    }
    
    Box(modifier = modifier) {
        AndroidView(
            factory = { 
                mapView 
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                // Asegurar que el mapa tenga tamaño válido
                if (view.layoutParams == null || 
                    view.layoutParams.width != android.view.ViewGroup.LayoutParams.MATCH_PARENT ||
                    view.layoutParams.height != android.view.ViewGroup.LayoutParams.MATCH_PARENT) {
                    view.layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }
        )
        
        // Mostrar ETA si está disponible
        if (showETA && routeResult != null && workerLatitude != null && workerLongitude != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = RegisterColors.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = RegisterColors.PrimaryOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = routeService.formatDuration(routeResult!!.duration),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RegisterColors.DarkGray
                    )
                    Text(
                        text = "•",
                        color = RegisterColors.TextGray
                    )
                    Text(
                        text = routeService.formatDistance(routeResult!!.distance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = RegisterColors.TextGray
                    )
                }
            }
        }
        
        // Botón flotante para iniciar navegación
        if (onStartNavigation != null && workerLatitude != null && workerLongitude != null) {
            FloatingActionButton(
                onClick = onStartNavigation,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = RegisterColors.PrimaryOrange
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = "Iniciar navegación",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Crea un icono personalizado para marcadores
 */
private fun createCustomMarkerIcon(
    context: android.content.Context,
    color: Color,
    size: Int = 60
): android.graphics.drawable.Drawable {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Dibujar círculo exterior
    val outerPaint = Paint().apply {
        isAntiAlias = true
        setColor(color.hashCode())
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, outerPaint)
    
    // Dibujar círculo interior (blanco)
    val innerPaint = Paint().apply {
        isAntiAlias = true
        setColor(Color.White.hashCode())
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 3f, innerPaint)
    
    // Dibujar punto central
    val centerPaint = Paint().apply {
        isAntiAlias = true
        setColor(color.hashCode())
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 6f, centerPaint)
    
    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Calcula la distancia entre dos puntos usando la fórmula de Haversine
 */
fun calculateDistance(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Float {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0]
}

/**
 * Formatea la distancia en formato legible
 */
fun formatDistance(distanceInMeters: Float): String {
    return when {
        distanceInMeters < 1000 -> "${distanceInMeters.toInt()} m"
        else -> String.format("%.2f km", distanceInMeters / 1000)
    }
}

