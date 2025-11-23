package com.example.getjob.presentation.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun OSMMapView(
    latitude: Double,
    longitude: Double,
    address: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val geocodingService = remember { com.example.getjob.utils.GeocodingService() }
    var resolvedAddress by remember { mutableStateOf(address) }
    
    // Configurar OSMDroid de manera optimizada (solo una vez)
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
            setTileSource(TileSourceFactory.MAPNIK) // Usar tiles de OpenStreetMap
            setMultiTouchControls(true) // Habilitar zoom con pellizco
            minZoomLevel = 5.0
            maxZoomLevel = 19.0
            
            // Optimizaciones de renderizado
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
            isClickable = true
            isFocusable = true
            
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
    
    // Obtener dirección correcta desde coordenadas (reverse geocoding)
    // Esto asegura que siempre mostremos la dirección más precisa basada en las coordenadas reales
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(latitude, longitude) {
        // Siempre intentar obtener la dirección desde las coordenadas para mayor precisión
        coroutineScope.launch {
            val reverseAddress = geocodingService.reverseGeocode(latitude, longitude)
            if (reverseAddress != null) {
                resolvedAddress = reverseAddress
                android.util.Log.d("OSMMapView", "Dirección obtenida desde coordenadas: $reverseAddress")
            } else {
                // Si falla el reverse geocoding, usar la dirección proporcionada
                resolvedAddress = address.ifEmpty { "Ubicación" }
                android.util.Log.w("OSMMapView", "No se pudo obtener dirección desde coordenadas, usando: $address")
            }
        }
    }
    
    // Actualizar ubicación y marcador cuando cambien las coordenadas (en background)
    LaunchedEffect(latitude, longitude, resolvedAddress) {
        withContext(Dispatchers.Main) {
            try {
                mapView.overlays.clear()
                
                val geoPoint = GeoPoint(latitude, longitude)
                
                // Configurar la ubicación del mapa
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(geoPoint)
                
                // Agregar marcador con la dirección correcta
                val marker = Marker(mapView).apply {
                    position = geoPoint
                    title = resolvedAddress.ifEmpty { "Ubicación" }
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                
                mapView.overlays.add(marker)
                mapView.invalidate()
                
                // Forzar actualización del mapa
                mapView.post {
                    mapView.invalidate()
                }
            } catch (e: Exception) {
                android.util.Log.e("OSMMapView", "Error actualizando mapa: ${e.message}", e)
            }
        }
    }
    
    // Activar/desactivar mapa correctamente
    DisposableEffect(Unit) {
        // El mapa se activa automáticamente cuando se agrega a la vista
        onDispose {
            try {
                // Limpiar recursos si es necesario
            } catch (e: Exception) {
                // Ignorar errores al desmontar
            }
        }
    }
    
    AndroidView(
        factory = { 
            try {
                // Asegurar que el mapa esté correctamente inicializado
                mapView.onResume()
                mapView
            } catch (e: Exception) {
                android.util.Log.e("OSMMapView", "Error inicializando mapa: ${e.message}", e)
                mapView
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            try {
                // Asegurar que el mapa respete los límites del contenedor
                if (view.layoutParams == null || 
                    view.layoutParams.width != android.view.ViewGroup.LayoutParams.MATCH_PARENT ||
                    view.layoutParams.height != android.view.ViewGroup.LayoutParams.MATCH_PARENT) {
                    view.layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                // Forzar actualización
                view.post {
                    view.invalidate()
                }
            } catch (e: Exception) {
                android.util.Log.e("OSMMapView", "Error actualizando vista: ${e.message}", e)
            }
        }
    )
}

